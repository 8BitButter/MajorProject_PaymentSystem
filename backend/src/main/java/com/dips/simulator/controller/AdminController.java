package com.dips.simulator.controller;

import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.dto.LoadProfileRequest;
import com.dips.simulator.service.FailureInjectionService;
import com.dips.simulator.service.LoadProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final FailureInjectionService failureService;
    private final LoadProfileService loadProfileService;

    public AdminController(FailureInjectionService failureService, LoadProfileService loadProfileService) {
        this.failureService = failureService;
        this.loadProfileService = loadProfileService;
    }

    @PostMapping("/failure-scenarios/{scenario}/enable")
    public Map<String, Object> enable(@PathVariable FailureScenario scenario) {
        failureService.set(scenario, true);
        return Map.of("scenario", scenario, "enabled", true);
    }

    @PostMapping("/failure-scenarios/{scenario}/disable")
    public Map<String, Object> disable(@PathVariable FailureScenario scenario) {
        failureService.set(scenario, false);
        return Map.of("scenario", scenario, "enabled", false);
    }

    @PostMapping("/load-profile")
    public Map<String, Object> loadProfile(@Valid @RequestBody LoadProfileRequest request) {
        loadProfileService.setProfile(request.getProfile());
        return Map.of("profile", request.getProfile());
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "loadProfile", loadProfileService.getProfile(),
                "failureScenarios", failureService.snapshot()
        );
    }
}

