package com.dips.simulator.service;

import com.dips.simulator.domain.IdempotencyRecordEntity;
import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.TransactionEventEntity;
import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.domain.enums.StageBranch;
import com.dips.simulator.domain.enums.TransactionSource;
import com.dips.simulator.domain.enums.TransactionState;
import com.dips.simulator.dto.PushPaymentRequest;
import com.dips.simulator.dto.PushPaymentResponse;
import com.dips.simulator.dto.StageDecision;
import com.dips.simulator.dto.TransactionEventResponse;
import com.dips.simulator.dto.TransactionResponse;
import com.dips.simulator.dto.TransactionStepResponse;
import com.dips.simulator.repository.TransactionEventRepository;
import com.dips.simulator.repository.TransactionRepository;
import com.dips.simulator.service.scheduler.PrioritySchedulerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final IdempotencyService idempotencyService;
    private final PrioritySchedulerService schedulerService;
    private final TransactionStateService stateService;
    private final TransactionStepService transactionStepService;
    private final PspStageService pspStageService;
    private final StageDelayService stageDelayService;

    public PaymentService(
            TransactionRepository transactionRepository,
            TransactionEventRepository eventRepository,
            IdempotencyService idempotencyService,
            PrioritySchedulerService schedulerService,
            TransactionStateService stateService,
            TransactionStepService transactionStepService,
            PspStageService pspStageService,
            StageDelayService stageDelayService
    ) {
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.idempotencyService = idempotencyService;
        this.schedulerService = schedulerService;
        this.stateService = stateService;
        this.transactionStepService = transactionStepService;
        this.pspStageService = pspStageService;
        this.stageDelayService = stageDelayService;
    }

    @Transactional
    public PushPaymentResponse initiateOnlinePush(PushPaymentRequest request) {
        return initiate(request, TransactionSource.ONLINE, false);
    }

    @Transactional
    public PushPaymentResponse initiateOfflinePush(PushPaymentRequest request) {
        return initiate(request, TransactionSource.OFFLINE_SMS, true);
    }

    public TransactionResponse getTransaction(UUID transactionId) {
        TransactionEntity tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new DomainException("Transaction not found: " + transactionId));
        return toResponse(tx, true);
    }

    public List<TransactionEventResponse> getEvents(UUID transactionId) {
        return eventRepository.findByTransactionIdOrderByIdAsc(transactionId).stream()
                .map(this::toEventResponse)
                .toList();
    }

    public List<TransactionStepResponse> getTimeline(UUID transactionId) {
        return transactionStepService.getTimeline(transactionId);
    }

    private PushPaymentResponse initiate(PushPaymentRequest request, TransactionSource source, boolean offline) {
        IdempotencyRecordEntity existing = idempotencyService.find(request.getPayerVpa(), request.getClientRequestId()).orElse(null);
        if (existing != null) {
            idempotencyService.ensureSameRequest(existing, request);
            TransactionEntity existingTx = transactionRepository.findById(existing.getTransactionId())
                    .orElseThrow(() -> new DomainException("Idempotency record references missing transaction"));
            return new PushPaymentResponse(existingTx.getId(), true, existingTx.getState(), OffsetDateTime.now());
        }

        TransactionEntity tx = new TransactionEntity();
        tx.setId(UUID.randomUUID());
        tx.setClientRequestId(request.getClientRequestId());
        tx.setCorrelationId("corr-" + tx.getId());
        tx.setPayerVpa(request.getPayerVpa());
        tx.setPayeeVpa(request.getPayeeVpa());
        tx.setPayerMpin(request.getMpin());
        tx.setAmount(request.getAmount());
        tx.setSource(source);
        tx.setState(TransactionState.CREATED);
        tx.setTerminal(false);
        tx.setCreatedAt(OffsetDateTime.now());
        tx.setUpdatedAt(OffsetDateTime.now());
        stateService.recordInitial(tx, "PSP", "Transaction created");

        if (offline) {
            stateService.transition(tx, TransactionState.OFFLINE_QUEUED, "SMS_GATEWAY", "Queued via offline SMS");
            stateService.transition(tx, TransactionState.OFFLINE_RECEIVED, "SMS_GATEWAY", "SMS received by PSP");
            stateService.transition(tx, TransactionState.OFFLINE_DECRYPTED, "PSP", "SMS payload decrypted");
        }

        OffsetDateTime stepStart = OffsetDateTime.now();
        long processing = stageDelayService.sleepFor(tx.getId(), PaymentStage.INITIATE);
        OffsetDateTime stepEnd = OffsetDateTime.now();
        StageDecision decision = pspStageService.initiateDecision(tx);
        decision.setProcessingMs(processing);
        decision.setBranch(StageBranch.MAIN);
        transactionStepService.record(
                tx,
                decision,
                stepStart,
                stepEnd,
                "source=" + tx.getSource(),
                "accepted=true"
        );

        idempotencyService.store(request, tx);
        schedulerService.enqueue(tx.getId(), tx.getAmount());
        return new PushPaymentResponse(tx.getId(), false, tx.getState(), OffsetDateTime.now());
    }

    private TransactionResponse toResponse(TransactionEntity tx, boolean includeEvents) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(tx.getId());
        response.setClientRequestId(tx.getClientRequestId());
        response.setCorrelationId(tx.getCorrelationId());
        response.setPayerVpa(tx.getPayerVpa());
        response.setPayeeVpa(tx.getPayeeVpa());
        response.setAmount(tx.getAmount());
        response.setSource(tx.getSource());
        response.setState(tx.getState());
        response.setTerminal(tx.isTerminal());
        response.setCreatedAt(tx.getCreatedAt());
        response.setUpdatedAt(tx.getUpdatedAt());
        if (includeEvents) {
            response.setEvents(getEvents(tx.getId()));
        }
        return response;
    }

    private TransactionEventResponse toEventResponse(TransactionEventEntity event) {
        TransactionEventResponse response = new TransactionEventResponse();
        response.setId(event.getId());
        response.setFromState(event.getFromState());
        response.setToState(event.getToState());
        response.setActor(event.getActor());
        response.setReason(event.getReason());
        response.setCreatedAt(event.getCreatedAt());
        return response;
    }
}
