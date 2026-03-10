package com.dips.simulator.repository;

import com.dips.simulator.domain.BankNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankNodeRepository extends JpaRepository<BankNodeEntity, Long> {
    Optional<BankNodeEntity> findByCode(String code);
}

