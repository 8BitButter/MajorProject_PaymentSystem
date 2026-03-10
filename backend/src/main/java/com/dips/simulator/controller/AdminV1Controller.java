package com.dips.simulator.controller;

import com.dips.simulator.domain.enums.BankType;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.dto.AdminAccountResponse;
import com.dips.simulator.dto.DelayProfileRequest;
import com.dips.simulator.dto.DelayProfileResponse;
import com.dips.simulator.dto.TransactionStepResponse;
import com.dips.simulator.service.AdminQueryService;
import com.dips.simulator.service.FailureInjectionService;
import com.dips.simulator.service.PaymentService;
import com.dips.simulator.service.StageDelayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminV1Controller {

    private final FailureInjectionService failureInjectionService;
    private final StageDelayService stageDelayService;
    private final PaymentService paymentService;
    private final AdminQueryService adminQueryService;

    public AdminV1Controller(
            FailureInjectionService failureInjectionService,
            StageDelayService stageDelayService,
            PaymentService paymentService,
            AdminQueryService adminQueryService
    ) {
        this.failureInjectionService = failureInjectionService;
        this.stageDelayService = stageDelayService;
        this.paymentService = paymentService;
        this.adminQueryService = adminQueryService;
    }

    @PostMapping("/failures/{scenario}/enable")
    public Map<String, Object> enableFailure(@PathVariable FailureScenario scenario) {
        failureInjectionService.set(scenario, true);
        return Map.of("scenario", scenario, "enabled", true);
    }

    @PostMapping("/failures/{scenario}/disable")
    public Map<String, Object> disableFailure(@PathVariable FailureScenario scenario) {
        failureInjectionService.set(scenario, false);
        return Map.of("scenario", scenario, "enabled", false);
    }

    @PostMapping("/delay-profile")
    public DelayProfileResponse setDelayProfile(@Valid @RequestBody DelayProfileRequest request) {
        return stageDelayService.updateProfile(request.getBaseDelayMs(), request.getJitterMs());
    }

    @GetMapping("/delay-profile")
    public DelayProfileResponse getDelayProfile() {
        return stageDelayService.getProfile();
    }

    @GetMapping("/transactions/{txId}/steps")
    public List<TransactionStepResponse> transactionSteps(@PathVariable UUID txId) {
        return paymentService.getTimeline(txId);
    }

    @GetMapping("/accounts")
    public List<AdminAccountResponse> accounts(
            @RequestParam(required = false) String userVpa,
            @RequestParam(required = false) BankType bankType
    ) {
        return adminQueryService.listAccounts(userVpa, bankType);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "delayProfile", stageDelayService.getProfile(),
                "failureScenarios", failureInjectionService.snapshot()
        );
    }
}

