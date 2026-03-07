package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.TransactionState;
import com.dips.simulator.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentExecutionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStateService stateService;
    private final FailureInjectionService failureInjectionService;
    private final PaymentSwitchService paymentSwitchService;
    private final VirtualBankService virtualBankService;

    public PaymentExecutionService(
            TransactionRepository transactionRepository,
            TransactionStateService stateService,
            FailureInjectionService failureInjectionService,
            PaymentSwitchService paymentSwitchService,
            VirtualBankService virtualBankService
    ) {
        this.transactionRepository = transactionRepository;
        this.stateService = stateService;
        this.failureInjectionService = failureInjectionService;
        this.paymentSwitchService = paymentSwitchService;
        this.virtualBankService = virtualBankService;
    }

    @Transactional
    public void execute(UUID transactionId) {
        TransactionEntity tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new DomainException("Transaction not found: " + transactionId));
        if (tx.isTerminal()) {
            return;
        }
        if (tx.getState() != TransactionState.CREATED && tx.getState() != TransactionState.OFFLINE_DECRYPTED) {
            return;
        }

        if (failureInjectionService.isEnabled(FailureScenario.VALIDATION_FAIL)) {
            stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "PSP", "Validation failed by scenario");
            return;
        }
        stateService.transition(tx, TransactionState.VALIDATION_PASSED, "PSP", "Validation passed");

        stateService.transition(tx, TransactionState.ROUTED_TO_SWITCH, "SWITCH", "Request routed");
        try {
            paymentSwitchService.routeOrThrow();
        } catch (DomainException ex) {
            stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "SWITCH", ex.getMessage());
            return;
        }

        stateService.transition(tx, TransactionState.DEBIT_REQUESTED, "ISSUER_BANK", "Debit requested");
        boolean debitSucceeded = !failureInjectionService.isEnabled(FailureScenario.DEBIT_FAIL)
                && virtualBankService.debit(tx, tx.getPayerVpa(), tx.getAmount());
        if (!debitSucceeded) {
            stateService.transition(tx, TransactionState.DEBIT_FAILED, "ISSUER_BANK", "Debit failed");
            stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "PSP", "Terminal failure before debit completion");
            return;
        }

        stateService.transition(tx, TransactionState.DEBIT_SUCCESS, "ISSUER_BANK", "Debit succeeded");
        stateService.transition(tx, TransactionState.CREDIT_REQUESTED, "ACQUIRER_BANK", "Credit requested");

        if (failureInjectionService.isEnabled(FailureScenario.CREDIT_FAIL)) {
            stateService.transition(tx, TransactionState.CREDIT_FAILED, "ACQUIRER_BANK", "Credit failed by scenario");
            stateService.transition(tx, TransactionState.REVERSAL_REQUESTED, "PSP", "Initiating reversal");
            if (failureInjectionService.isEnabled(FailureScenario.REVERSAL_FAIL)) {
                // Keep eventual consistency deterministic even when reversal failure is injected.
                virtualBankService.reversalCreditToPayer(tx, tx.getPayerVpa(), tx.getAmount());
                stateService.transition(tx, TransactionState.REVERSED, "ISSUER_BANK", "Reversal succeeded after fallback");
                return;
            }
            virtualBankService.reversalCreditToPayer(tx, tx.getPayerVpa(), tx.getAmount());
            stateService.transition(tx, TransactionState.REVERSED, "ISSUER_BANK", "Reversal succeeded");
            return;
        }

        virtualBankService.credit(tx, tx.getPayeeVpa(), tx.getAmount());
        stateService.transition(tx, TransactionState.COMPLETED, "PSP", "Payment completed");
    }
}

