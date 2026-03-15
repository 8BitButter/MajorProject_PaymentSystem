package com.dips.simulator.repository;

import com.dips.simulator.domain.ProcessedOperationEntity;
import com.dips.simulator.domain.enums.BankOperationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedOperationRepository extends JpaRepository<ProcessedOperationEntity, Long> {
    Optional<ProcessedOperationEntity> findByTransactionIdAndOperationType(UUID transactionId, BankOperationType operationType);
}
