package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.domain.enums.StageBranch;
import com.dips.simulator.domain.enums.StageStatus;
import com.dips.simulator.dto.StageDecision;
import org.springframework.stereotype.Service;

@Service
public class AcquirerStageService {

    private final FailureInjectionService failureInjectionService;
    private final VirtualBankService virtualBankService;

    public AcquirerStageService(
            FailureInjectionService failureInjectionService,
            VirtualBankService virtualBankService
    ) {
        this.failureInjectionService = failureInjectionService;
        this.virtualBankService = virtualBankService;
    }

    public StageDecision creditDecision(TransactionEntity tx, boolean apply) {
        StageDecision decision = new StageDecision();
        decision.setStage(PaymentStage.CREDIT);
        decision.setActor("ACQUIRER_BANK");

        if (failureInjectionService.isEnabled(FailureScenario.CREDIT_FAIL)) {
            decision.setStatus(StageStatus.FAIL);
            decision.setReason("Credit failure injected");
            decision.setNextStage(PaymentStage.REVERSAL);
            decision.setBranch(StageBranch.FAILURE);
            return decision;
        }

        if (apply) {
            virtualBankService.credit(tx, tx.getPayeeVpa(), tx.getAmount());
        }
        decision.setStatus(StageStatus.PROCEED);
        decision.setReason("Credit success");
        decision.setNextStage(PaymentStage.TERMINAL);
        decision.setBranch(StageBranch.MAIN);
        return decision;
    }
}

