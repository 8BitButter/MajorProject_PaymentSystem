package com.dips.simulator.service;

import com.dips.simulator.domain.enums.FailureScenario;
import org.springframework.stereotype.Service;

@Service
public class PaymentSwitchService {

    private final FailureInjectionService failureInjectionService;

    public PaymentSwitchService(FailureInjectionService failureInjectionService) {
        this.failureInjectionService = failureInjectionService;
    }

    public void routeOrThrow() {
        if (failureInjectionService.isEnabled(FailureScenario.SWITCH_FAIL)) {
            throw new DomainException("Switch routing failure injected");
        }
    }
}

