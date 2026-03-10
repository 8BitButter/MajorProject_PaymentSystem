package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.domain.enums.StageStatus;
import com.dips.simulator.domain.enums.TransactionState;
import com.dips.simulator.dto.StageDecision;
import com.dips.simulator.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentExecutionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStateService stateService;
    private final PspStageService pspStageService;
    private final SwitchStageService switchStageService;
    private final IssuerStageService issuerStageService;
    private final AcquirerStageService acquirerStageService;
    private final StageDelayService stageDelayService;
    private final TransactionStepService transactionStepService;

    public PaymentExecutionService(
            TransactionRepository transactionRepository,
            TransactionStateService stateService,
            PspStageService pspStageService,
            SwitchStageService switchStageService,
            IssuerStageService issuerStageService,
            AcquirerStageService acquirerStageService,
            StageDelayService stageDelayService,
            TransactionStepService transactionStepService
    ) {
        this.transactionRepository = transactionRepository;
        this.stateService = stateService;
        this.pspStageService = pspStageService;
        this.switchStageService = switchStageService;
        this.issuerStageService = issuerStageService;
        this.acquirerStageService = acquirerStageService;
        this.stageDelayService = stageDelayService;
        this.transactionStepService = transactionStepService;
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

        StageDecision validateDecision = runStage(
                tx,
                PaymentStage.VALIDATE,
                () -> pspStageService.validateDecision(tx),
                "payer=" + tx.getPayerVpa()
        );
        if (validateDecision.getStatus() == StageStatus.FAIL) {
            stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, validateDecision.getActor(), validateDecision.getReason());
            return;
        }
        stateService.transition(tx, TransactionState.VALIDATION_PASSED, validateDecision.getActor(), validateDecision.getReason());

        StageDecision routeDecision = runStage(
                tx,
                PaymentStage.ROUTE,
                () -> switchStageService.routeDecision(tx),
                "txId=" + tx.getId()
        );
        if (routeDecision.getStatus() == StageStatus.FAIL) {
            stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, routeDecision.getActor(), routeDecision.getReason());
            return;
        }
        stateService.transition(tx, TransactionState.ROUTED_TO_SWITCH, routeDecision.getActor(), routeDecision.getReason());

        stateService.transition(tx, TransactionState.DEBIT_REQUESTED, "ISSUER_BANK", "Debit requested");
        StageDecision debitDecision = runStage(
                tx,
                PaymentStage.DEBIT,
                () -> issuerStageService.debitDecision(tx, true),
                "amount=" + tx.getAmount()
        );
        if (debitDecision.getStatus() == StageStatus.FAIL) {
            stateService.transition(tx, TransactionState.DEBIT_FAILED, debitDecision.getActor(), debitDecision.getReason());
            stateService.transition(tx, TransactionState.FAILED_PRE_DEBIT, "PSP", "Terminal failure before debit completion");
            return;
        }

        stateService.transition(tx, TransactionState.DEBIT_SUCCESS, debitDecision.getActor(), debitDecision.getReason());
        stateService.transition(tx, TransactionState.CREDIT_REQUESTED, "ACQUIRER_BANK", "Credit requested");

        StageDecision creditDecision = runStage(
                tx,
                PaymentStage.CREDIT,
                () -> acquirerStageService.creditDecision(tx, true),
                "amount=" + tx.getAmount()
        );
        if (creditDecision.getStatus() == StageStatus.FAIL) {
            stateService.transition(tx, TransactionState.CREDIT_FAILED, creditDecision.getActor(), creditDecision.getReason());
            stateService.transition(tx, TransactionState.REVERSAL_REQUESTED, "PSP", "Initiating reversal");
            StageDecision reversalDecision = runStage(
                    tx,
                    PaymentStage.REVERSAL,
                    () -> issuerStageService.reversalDecision(tx, true),
                    "amount=" + tx.getAmount()
            );
            stateService.transition(tx, TransactionState.REVERSED, reversalDecision.getActor(), reversalDecision.getReason());
            return;
        }

        stateService.transition(tx, TransactionState.COMPLETED, "PSP", "Payment completed");
    }

    private StageDecision runStage(
            TransactionEntity tx,
            PaymentStage stage,
            java.util.function.Supplier<StageDecision> supplier,
            String inputSummary
    ) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        long delayMs = stageDelayService.sleepFor(tx.getId(), stage);
        StageDecision decision = supplier.get();
        decision.setProcessingMs(delayMs);
        OffsetDateTime endedAt = OffsetDateTime.now();
        transactionStepService.record(
                tx,
                decision,
                startedAt,
                endedAt,
                inputSummary,
                "status=" + decision.getStatus() + ",next=" + decision.getNextStage()
        );
        return decision;
    }
}
