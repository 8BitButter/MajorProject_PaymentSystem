package com.dips.simulator.controller;

import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.dto.AdminDashboardResponse;
import com.dips.simulator.dto.LoadProfileRequest;
import com.dips.simulator.service.AdminDashboardService;
import com.dips.simulator.service.FailureInjectionService;
import com.dips.simulator.service.LoadProfileService;
import com.dips.simulator.service.scheduler.ExecutionQueueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final FailureInjectionService failureService;
    private final LoadProfileService loadProfileService;
    private final ExecutionQueueService executionQueueService;
    private final AdminDashboardService dashboardService;

    public AdminController(
            FailureInjectionService failureService,
            LoadProfileService loadProfileService,
            ExecutionQueueService executionQueueService,
            AdminDashboardService dashboardService
    ) {
        this.failureService = failureService;
        this.loadProfileService = loadProfileService;
        this.executionQueueService = executionQueueService;
        this.dashboardService = dashboardService;
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
                "failureScenarios", failureService.snapshot(),
                "executionQueue", executionQueueService.snapshot()
        );
    }
    
    @GetMapping("/logs")
    public Map<String, Object> logs() {
        // For demo: return static logs. Replace with real log fetching if available.
        return Map.of("logs", java.util.List.of(
                Map.of("timestamp", java.time.OffsetDateTime.now().minusMinutes(2).toString(), "level", "INFO", "user", "dips", "action", "LOGIN", "message", "Admin logged in"),
                Map.of("timestamp", java.time.OffsetDateTime.now().minusMinutes(1).toString(), "level", "INFO", "user", "dips", "action", "DASHBOARD_VIEW", "message", "Viewed dashboard"),
                Map.of("timestamp", java.time.OffsetDateTime.now().toString(), "level", "WARN", "user", "dips", "action", "FAILURE_INJECTION", "message", "Enabled DEBIT_FAIL scenario")
        ));
    }
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        // For demo: return static health info. Replace with real health checks if available.
        return Map.of(
            "status", "UP",
            "uptime", "2h 15m",
            "dbStatus", "UP",
            "queueDepth", 3,
            "lastEvent", java.time.OffsetDateTime.now().toString()
        );
    }
    
    @GetMapping("/failure-scenarios")
    public Map<String, Boolean> failureScenarios() {
        // Return all failure scenarios and their enabled/disabled state
        return failureService.snapshot().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String userId
    ) {
        return dashboardService.dashboard(limit, userId);
    }
}

