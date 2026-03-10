package com.dips.simulator.service;

import com.dips.simulator.domain.FailureConfigEntity;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.repository.FailureConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
public class FailureInjectionService {

    private final Map<FailureScenario, Boolean> enabled = new EnumMap<>(FailureScenario.class);
    private final FailureConfigRepository failureConfigRepository;

    public FailureInjectionService(FailureConfigRepository failureConfigRepository) {
        this.failureConfigRepository = failureConfigRepository;
        for (FailureScenario scenario : FailureScenario.values()) {
            enabled.put(scenario, false);
        }
    }

    @PostConstruct
    @Transactional
    public void init() {
        for (FailureScenario scenario : FailureScenario.values()) {
            FailureConfigEntity row = failureConfigRepository.findByScenario(scenario).orElseGet(() -> {
                FailureConfigEntity entity = new FailureConfigEntity();
                entity.setScenario(scenario);
                entity.setEnabled(false);
                entity.setUpdatedAt(OffsetDateTime.now());
                return failureConfigRepository.save(entity);
            });
            enabled.put(scenario, row.isEnabled());
        }
    }

    @Transactional
    public void set(FailureScenario scenario, boolean value) {
        enabled.put(scenario, value);
        FailureConfigEntity row = failureConfigRepository.findByScenario(scenario).orElseGet(() -> {
            FailureConfigEntity entity = new FailureConfigEntity();
            entity.setScenario(scenario);
            return entity;
        });
        row.setEnabled(value);
        row.setUpdatedAt(OffsetDateTime.now());
        failureConfigRepository.save(row);
    }

    public boolean isEnabled(FailureScenario scenario) {
        return enabled.getOrDefault(scenario, false);
    }

    public Map<FailureScenario, Boolean> snapshot() {
        return Map.copyOf(enabled);
    }
}
