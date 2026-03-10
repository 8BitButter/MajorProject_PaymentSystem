package com.dips.simulator.repository;

import com.dips.simulator.domain.TransactionStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionStepRepository extends JpaRepository<TransactionStepEntity, Long> {
    List<TransactionStepEntity> findByTransactionIdOrderByIdAsc(UUID transactionId);
}

