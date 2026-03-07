package com.dips.simulator.service;

import com.dips.simulator.domain.enums.FailureScenario;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class FailureInjectionService {

    private final Map<FailureScenario, Boolean> enabled = new EnumMap<>(FailureScenario.class);

    public FailureInjectionService() {
        for (FailureScenario scenario : FailureScenario.values()) {
            enabled.put(scenario, false);
        }
    }

    public void set(FailureScenario scenario, boolean value) {
        enabled.put(scenario, value);
    }

    public boolean isEnabled(FailureScenario scenario) {
        return enabled.getOrDefault(scenario, false);
    }

    public Map<FailureScenario, Boolean> snapshot() {
        return Map.copyOf(enabled);
    }
}

