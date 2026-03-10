package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.domain.enums.StageBranch;
import com.dips.simulator.domain.enums.StageStatus;
import com.dips.simulator.dto.StageDecision;
import org.springframework.stereotype.Service;

@Service
public class IssuerStageService {

    private final FailureInjectionService failureInjectionService;
    private final VirtualBankService virtualBankService;

    public IssuerStageService(
            FailureInjectionService failureInjectionService,
            VirtualBankService virtualBankService
    ) {
        this.failureInjectionService = failureInjectionService;
        this.virtualBankService = virtualBankService;
    }

    public StageDecision debitDecision(TransactionEntity tx, boolean apply) {
        StageDecision decision = new StageDecision();
        decision.setStage(PaymentStage.DEBIT);
        decision.setActor("ISSUER_BANK");

        if (failureInjectionService.isEnabled(FailureScenario.DEBIT_FAIL)) {
            decision.setStatus(StageStatus.FAIL);
            decision.setReason("Debit failure injected");
            decision.setNextStage(PaymentStage.TERMINAL);
            decision.setBranch(StageBranch.FAILURE);
            return decision;
        }
        if (!apply) {
            decision.setStatus(StageStatus.PROCEED);
            decision.setReason("Debit would proceed");
            decision.setNextStage(PaymentStage.CREDIT);
            decision.setBranch(StageBranch.MAIN);
            return decision;
        }

        boolean ok = virtualBankService.debit(tx, tx.getPayerVpa(), tx.getAmount());
        if (!ok) {
            decision.setStatus(StageStatus.FAIL);
            decision.setReason("Insufficient balance");
            decision.setNextStage(PaymentStage.TERMINAL);
            decision.setBranch(StageBranch.FAILURE);
            return decision;
        }

        decision.setStatus(StageStatus.PROCEED);
        decision.setReason("Debit success");
        decision.setNextStage(PaymentStage.CREDIT);
        decision.setBranch(StageBranch.MAIN);
        return decision;
    }

    public StageDecision reversalDecision(TransactionEntity tx, boolean apply) {
        StageDecision decision = new StageDecision();
        decision.setStage(PaymentStage.REVERSAL);
        decision.setActor("ISSUER_BANK");

        if (apply) {
            virtualBankService.reversalCreditToPayer(tx, tx.getPayerVpa(), tx.getAmount());
        }

        if (failureInjectionService.isEnabled(FailureScenario.REVERSAL_FAIL)) {
            decision.setStatus(StageStatus.PROCEED);
            decision.setReason("Reversal fallback succeeded");
            decision.setNextStage(PaymentStage.TERMINAL);
            decision.setBranch(StageBranch.REVERSAL);
            return decision;
        }
        decision.setStatus(StageStatus.PROCEED);
        decision.setReason("Reversal success");
        decision.setNextStage(PaymentStage.TERMINAL);
        decision.setBranch(StageBranch.REVERSAL);
        return decision;
    }
}

