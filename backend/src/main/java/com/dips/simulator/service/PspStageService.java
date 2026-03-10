package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.domain.enums.StageBranch;
import com.dips.simulator.domain.enums.StageStatus;
import com.dips.simulator.dto.StageDecision;
import org.springframework.stereotype.Service;

@Service
public class PspStageService {

    private final UserDirectoryService userDirectoryService;
    private final FailureInjectionService failureInjectionService;

    public PspStageService(
            UserDirectoryService userDirectoryService,
            FailureInjectionService failureInjectionService
    ) {
        this.userDirectoryService = userDirectoryService;
        this.failureInjectionService = failureInjectionService;
    }

    public StageDecision initiateDecision(TransactionEntity tx) {
        StageDecision decision = new StageDecision();
        decision.setStage(PaymentStage.INITIATE);
        decision.setStatus(StageStatus.PROCEED);
        decision.setActor("PSP");
        decision.setReason("Push payment accepted");
        decision.setNextStage(PaymentStage.VALIDATE);
        decision.setBranch(StageBranch.MAIN);
        return decision;
    }

    public StageDecision validateDecision(TransactionEntity tx) {
        StageDecision decision = new StageDecision();
        decision.setStage(PaymentStage.VALIDATE);
        decision.setActor("PSP");

        if (failureInjectionService.isEnabled(FailureScenario.VALIDATION_FAIL)) {
            decision.setStatus(StageStatus.FAIL);
            decision.setReason("Validation failed by scenario");
            decision.setNextStage(PaymentStage.TERMINAL);
            decision.setBranch(StageBranch.FAILURE);
            return decision;
        }

        if (userDirectoryService.findByVpa(tx.getPayerVpa()).isEmpty()) {
            decision.setStatus(StageStatus.FAIL);
            decision.setReason("Payer VPA not found");
            decision.setNextStage(PaymentStage.TERMINAL);
            decision.setBranch(StageBranch.FAILURE);
            return decision;
        }
        if (userDirectoryService.findByVpa(tx.getPayeeVpa()).isEmpty()) {
            decision.setStatus(StageStatus.FAIL);
            decision.setReason("Payee VPA not found");
            decision.setNextStage(PaymentStage.TERMINAL);
            decision.setBranch(StageBranch.FAILURE);
            return decision;
        }
        if (!userDirectoryService.verifyMpin(tx.getPayerVpa(), tx.getPayerMpin())) {
            decision.setStatus(StageStatus.FAIL);
            decision.setReason("Invalid MPIN");
            decision.setNextStage(PaymentStage.TERMINAL);
            decision.setBranch(StageBranch.FAILURE);
            return decision;
        }

        decision.setStatus(StageStatus.PROCEED);
        decision.setReason("Validation passed");
        decision.setNextStage(PaymentStage.ROUTE);
        decision.setBranch(StageBranch.MAIN);
        return decision;
    }
}

