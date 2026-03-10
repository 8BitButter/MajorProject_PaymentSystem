package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.domain.enums.StageBranch;
import com.dips.simulator.domain.enums.StageStatus;
import com.dips.simulator.dto.StageDecision;
import org.springframework.stereotype.Service;

@Service
public class SwitchStageService {

    private final FailureInjectionService failureInjectionService;

    public SwitchStageService(FailureInjectionService failureInjectionService) {
        this.failureInjectionService = failureInjectionService;
    }

    public StageDecision routeDecision(TransactionEntity tx) {
        StageDecision decision = new StageDecision();
        decision.setStage(PaymentStage.ROUTE);
        decision.setActor("SWITCH");

        if (failureInjectionService.isEnabled(FailureScenario.SWITCH_FAIL)) {
            decision.setStatus(StageStatus.FAIL);
            decision.setReason("Switch routing failure injected");
            decision.setNextStage(PaymentStage.TERMINAL);
            decision.setBranch(StageBranch.FAILURE);
            return decision;
        }
        decision.setStatus(StageStatus.PROCEED);
        decision.setReason("Routing accepted");
        decision.setNextStage(PaymentStage.DEBIT);
        decision.setBranch(StageBranch.MAIN);
        return decision;
    }
}

