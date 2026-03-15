package com.dips.simulator.service;

import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.dto.BankOperationStatusResponse;
import com.dips.simulator.dto.OperationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentSwitchService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSwitchService.class);

    private final FailureInjectionService failureInjectionService;
    private final BankApiService bankApiService;

    public PaymentSwitchService(FailureInjectionService failureInjectionService, BankApiService bankApiService) {
        this.failureInjectionService = failureInjectionService;
        this.bankApiService = bankApiService;
    }

    public void routeOrThrow() {
        ensureRoutingAvailable();
    }

    public BankOperationStatusResponse routeDebit(OperationRequest request) {
        ensureRoutingAvailable();
        return bankApiService.debit(request);
    }

    public BankOperationStatusResponse routeCredit(OperationRequest request) {
        ensureRoutingAvailable();
        return bankApiService.credit(request);
    }

    public BankOperationStatusResponse routeReverse(OperationRequest request) {
        ensureRoutingAvailable();
        return bankApiService.reverse(request);
    }

    private void ensureRoutingAvailable() {
        log.info("event=switch_route_attempt");
        if (failureInjectionService.isEnabled(FailureScenario.SWITCH_TIMEOUT)
                || failureInjectionService.isEnabled(FailureScenario.NETWORK_TIMEOUT)) {
            log.warn("event=failure_injected stage=switch_routing scenario={}",
                    failureInjectionService.isEnabled(FailureScenario.SWITCH_TIMEOUT)
                            ? FailureScenario.SWITCH_TIMEOUT
                            : FailureScenario.NETWORK_TIMEOUT);
            throw new OperationTimeoutException("Switch routing timeout injected");
        }
        if (failureInjectionService.isEnabled(FailureScenario.SWITCH_FAIL)) {
            log.warn("event=failure_injected stage=switch_routing scenario={}", FailureScenario.SWITCH_FAIL);
            throw new DomainException("Switch routing failure injected");
        }
        log.info("event=switch_route_success");
    }
}

