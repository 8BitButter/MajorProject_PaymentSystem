package com.dips.simulator.repository;

import com.dips.simulator.domain.FailureConfigEntity;
import com.dips.simulator.domain.enums.FailureScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FailureConfigRepository extends JpaRepository<FailureConfigEntity, Long> {
    Optional<FailureConfigEntity> findByScenario(FailureScenario scenario);
}

