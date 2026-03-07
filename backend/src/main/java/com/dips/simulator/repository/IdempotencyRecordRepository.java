package com.dips.simulator.repository;

import com.dips.simulator.domain.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {
    Optional<IdempotencyRecordEntity> findByPayerVpaAndClientRequestId(String payerVpa, String clientRequestId);
}

