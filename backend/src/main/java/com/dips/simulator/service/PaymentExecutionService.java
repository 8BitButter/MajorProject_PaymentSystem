package com.dips.simulator.service;

import com.dips.simulator.config.SchedulerProperties;
import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.BankOperationStatus;
import com.dips.simulator.domain.enums.BankOperationType;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.TransactionState;
import com.dips.simulator.dto.BankOperationStatusResponse;
import com.dips.simulator.dto.OperationRequest;
import com.dips.simulator.logging.LogContext;
import com.dips.simulator.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExecutionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionStateService stateService;
    private final FailureInjectionService failureInjectionService;
    private final PaymentSwitchService paymentSwitchService;
    private final VirtualBankService virtualBankService;
    private final SchedulerProperties schedulerProperties;

    public PaymentExecutionService(
            TransactionRepository transactionRepository,
            TransactionStateService stateService,
            FailureInjectionService failureInjectionService,
            PaymentSwitchService paymentSwitchService,
            VirtualBankService virtualBankService,
            SchedulerProperties schedulerProperties
    ) {
        this.transactionRepository = transactionRepository;
        this.stateService = stateService;
        this.failureInjectionService = failureInjectionService;
        this.paymentSwitchService = paymentSwitchService;
        this.virtualBankService = virtualBankService;
        this.schedulerProperties = schedulerProperties;
    }

    @Transactional
    public void execute(UUID transactionId) {
        String previousCorrelation = LogContext.getCorrelationId();
        LogContext.setTransactionId(transactionId);
        TransactionEntity tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new DomainException("Transaction not found: " + transactionId));
        try {
            LogContext.setCorrelationId(tx.getCorrelationId());
            log.info("event=execution_started state={} source={}", tx.getState(), tx.getSource());
            if (tx.isTerminal()) {
                log.info("event=execution_skipped reason=already_terminal state={}", tx.getState());
                return;
            }
            if (tx.getState() != TransactionState.CREATED && tx.getState() != TransactionState.OFFLINE_DECRYPTED) {
                log.info("event=execution_skipped reason=unsupported_start_state state={}", tx.getState());
                return;
            }

            if (failureInjectionService.isEnabled(FailureScenario.VALIDATION_FAIL)) {
                log.warn("event=failure_injected stage=validation scenario={}", FailureScenario.VALIDATION_FAIL);
                stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "PSP", "Validation failed by scenario");
                return;
            }
            stateService.transition(tx, TransactionState.VALIDATION_PASSED, "PSP", "Validation passed");

            stateService.transition(tx, TransactionState.ROUTED_TO_SWITCH, "SWITCH", "Request routed");
            try {
                runWithDeterministicRetry("switch_route",
                        () -> {
                            paymentSwitchService.routeOrThrow();
                            return true;
                        },
                        () -> null);
            } catch (OperationTimeoutException ex) {
                stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "SWITCH", "Switch timeout after retries");
                return;
            } catch (DomainException ex) {
                stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "SWITCH", ex.getMessage());
                return;
            }

            stateService.transition(tx, TransactionState.DEBIT_REQUESTED, "ISSUER_BANK", "Debit requested");
            boolean debitSucceeded;
            try {
                OperationRequest debitRequest = operationRequest(tx, tx.getPayerVpa());
                debitSucceeded = runWithDeterministicRetry("debit",
                        () -> !failureInjectionService.isEnabled(FailureScenario.DEBIT_FAIL)
                                && isSuccess(paymentSwitchService.routeDebit(debitRequest)),
                        () -> resolveOperationStatus(tx.getId(), BankOperationType.DEBIT));
            } catch (OperationTimeoutException ex) {
                stateService.transition(tx, TransactionState.DEBIT_FAILED, "ISSUER_BANK", "Debit timeout after retries");
                stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "PSP", "Terminal failure after unresolved debit timeout");
                return;
            }
            if (!debitSucceeded) {
                stateService.transition(tx, TransactionState.DEBIT_FAILED, "ISSUER_BANK", "Debit failed");
                stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "PSP", "Terminal failure before debit completion");
                return;
            }

            stateService.transition(tx, TransactionState.DEBIT_SUCCESS, "ISSUER_BANK", "Debit succeeded");
            stateService.transition(tx, TransactionState.CREDIT_REQUESTED, "ACQUIRER_BANK", "Credit requested");

            if (failureInjectionService.isEnabled(FailureScenario.CREDIT_FAIL)) {
                log.warn("event=failure_injected stage=credit scenario={}", FailureScenario.CREDIT_FAIL);
                stateService.transition(tx, TransactionState.CREDIT_FAILED, "ACQUIRER_BANK", "Credit failed by scenario");
                stateService.transition(tx, TransactionState.REVERSAL_REQUESTED, "PSP", "Initiating reversal");
                runDeterministicReversal(tx);
                return;
            }

            boolean creditSucceeded;
            try {
                OperationRequest creditRequest = operationRequest(tx, tx.getPayeeVpa());
                creditSucceeded = runWithDeterministicRetry("credit",
                        () -> isSuccess(paymentSwitchService.routeCredit(creditRequest)),
                        () -> resolveOperationStatus(tx.getId(), BankOperationType.CREDIT));
            } catch (OperationTimeoutException ex) {
                creditSucceeded = false;
            }

            if (!creditSucceeded) {
                stateService.transition(tx, TransactionState.CREDIT_FAILED, "ACQUIRER_BANK", "Credit failed by timeout/retries");
                stateService.transition(tx, TransactionState.REVERSAL_REQUESTED, "PSP", "Initiating reversal after credit timeout");
                runDeterministicReversal(tx);
                return;
            }

            stateService.transition(tx, TransactionState.COMPLETED, "PSP", "Payment completed");
            log.info("event=execution_completed terminalState={}", tx.getState());
        } finally {
            LogContext.clearTransactionId();
            if (previousCorrelation == null || previousCorrelation.isBlank()) {
                LogContext.clearCorrelationId();
            } else {
                LogContext.setCorrelationId(previousCorrelation);
            }
        }
    }

    private void runDeterministicReversal(TransactionEntity tx) {
        OperationRequest reverseRequest = operationRequest(tx, tx.getPayerVpa());
        if (failureInjectionService.isEnabled(FailureScenario.REVERSAL_FAIL)) {
            log.warn("event=failure_injected stage=reversal scenario={}", FailureScenario.REVERSAL_FAIL);
            virtualBankService.reversalCreditToPayerForce(tx.getId(), tx.getPayerVpa(), tx.getAmount());
            stateService.transition(tx, TransactionState.REVERSED, "ISSUER_BANK", "Reversal succeeded after fallback");
            return;
        }

        boolean reversalSucceeded;
        try {
            reversalSucceeded = runWithDeterministicRetry("reversal",
                    () -> isSuccess(paymentSwitchService.routeReverse(reverseRequest)),
                    () -> resolveOperationStatus(tx.getId(), BankOperationType.REVERSAL));
        } catch (OperationTimeoutException ex) {
            reversalSucceeded = false;
        }

        if (!reversalSucceeded) {
            virtualBankService.reversalCreditToPayerForce(tx.getId(), tx.getPayerVpa(), tx.getAmount());
            stateService.transition(tx, TransactionState.REVERSED, "ISSUER_BANK", "Reversal forced after timeout exhaustion");
            return;
        }

        stateService.transition(tx, TransactionState.REVERSED, "ISSUER_BANK", "Reversal succeeded");
    }

    private boolean runWithDeterministicRetry(
            String operation,
            RetryableOperation operationCall,
            TimeoutStatusResolver statusResolver
    ) {
        int maxAttempts = Math.max(1, schedulerProperties.getOperationMaxRetries() + 1);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean result = operationCall.run();
                log.info("event=operation_attempt operation={} attempt={} timeoutMs={} result={}",
                        operation, attempt, schedulerProperties.getOperationTimeoutMs(), result);
                return result;
            } catch (OperationTimeoutException ex) {
                log.warn("event=operation_timeout operation={} attempt={} timeoutMs={}",
                        operation, attempt, schedulerProperties.getOperationTimeoutMs());
                Boolean resolved = statusResolver.resolve();
                if (resolved != null) {
                    log.info("event=operation_timeout_resolved operation={} attempt={} resolved={}",
                            operation, attempt, resolved);
                    return resolved;
                }
                if (attempt >= maxAttempts) {
                    log.warn("event=operation_timeout_exhausted operation={} attempts={}", operation, maxAttempts);
                    throw ex;
                }
                deterministicBackoff(operation, attempt);
            }
        }
        return false;
    }

    private void deterministicBackoff(String operation, int attempt) {
        long delayMs = Math.max(0, schedulerProperties.getOperationRetryBackoffMs() * attempt);
        if (delayMs <= 0) {
            return;
        }
        log.info("event=operation_retry_backoff operation={} attempt={} backoffMs={}", operation, attempt, delayMs);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DomainException("Retry interrupted for operation: " + operation);
        }
    }

    private boolean isSuccess(BankOperationStatusResponse response) {
        return response != null && "SUCCESS".equalsIgnoreCase(response.getStatus());
    }

    private Boolean resolveOperationStatus(UUID transactionId, BankOperationType operationType) {
        BankOperationStatus status = virtualBankService.getOperationStatus(transactionId, operationType);
        return switch (status) {
            case SUCCESS -> Boolean.TRUE;
            case FAILED -> Boolean.FALSE;
            case NOT_FOUND -> null;
        };
    }

    private OperationRequest operationRequest(TransactionEntity tx, String accountId) {
        return new OperationRequest(tx.getId(), tx.getCorrelationId(), accountId, tx.getAmount());
    }

    @FunctionalInterface
    private interface RetryableOperation {
        boolean run();
    }

    @FunctionalInterface
    private interface TimeoutStatusResolver {
        Boolean resolve();
    }
}

