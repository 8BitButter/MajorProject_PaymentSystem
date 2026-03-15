package com.dips.simulator.service.scheduler;

import com.dips.simulator.config.SchedulerProperties;
import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.logging.LogContext;
import com.dips.simulator.service.LoadProfileService;
import com.dips.simulator.service.PaymentExecutionService;
import com.dips.simulator.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(value = "dips.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class PrioritySchedulerService implements TransactionDispatchService {

    private static final Logger log = LoggerFactory.getLogger(PrioritySchedulerService.class);

    private final SchedulerProperties schedulerProperties;
    private final LoadProfileService loadProfileService;
    private final PaymentExecutionService paymentExecutionService;
    private final ExecutionQueueService executionQueueService;
    private final TransactionRepository transactionRepository;
    private final String workerId = "psp-worker-" + UUID.randomUUID();

    public PrioritySchedulerService(
            SchedulerProperties schedulerProperties,
            LoadProfileService loadProfileService,
            PaymentExecutionService paymentExecutionService,
            ExecutionQueueService executionQueueService,
            TransactionRepository transactionRepository
    ) {
        this.schedulerProperties = schedulerProperties;
        this.loadProfileService = loadProfileService;
        this.paymentExecutionService = paymentExecutionService;
        this.executionQueueService = executionQueueService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void enqueue(UUID transactionId, BigDecimal amount) {
        executionQueueService.enqueue(transactionId, amount, schedulerProperties.getSmallValueThreshold());
    }

    @Scheduled(fixedDelayString = "${dips.scheduler.queue-poll-delay-ms:120}")
    public void processOne() {
        UUID transactionId = null;
        try {
            Optional<UUID> next = executionQueueService.claimNext(workerId);
            if (next.isEmpty()) {
                return;
            }
            transactionId = next.get();
            LogContext.setTransactionId(transactionId);

            paymentExecutionService.execute(transactionId);
            TransactionEntity tx = transactionRepository.findById(transactionId).orElse(null);
            if (tx != null) {
                LogContext.setCorrelationId(tx.getCorrelationId());
            }
            if (tx == null || tx.isTerminal()) {
                executionQueueService.complete(transactionId);
                log.info("event=queue_complete profile={} terminal={}",
                        loadProfileService.getProfile(), tx == null || tx.isTerminal());
                return;
            }
            executionQueueService.releasePending(transactionId);
            log.info("event=queue_released_pending profile={} state={}",
                    loadProfileService.getProfile(), tx.getState());
        } catch (Exception ex) {
            if (transactionId == null) {
                log.error("Scheduler poll failed before claim/dispatch. profile={}", loadProfileService.getProfile(), ex);
                return;
            }
            String reason = ex.getClass().getSimpleName();
            if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
                reason = reason + ":" + ex.getMessage();
            }
            executionQueueService.requeueAfterFailure(transactionId, reason, schedulerProperties.getMaxRetryAttempts());
            log.warn("event=queue_execution_failed profile={} reason={}", loadProfileService.getProfile(), reason);
        } finally {
            LogContext.clearTransactionId();
            LogContext.clearCorrelationId();
        }
    }
}

