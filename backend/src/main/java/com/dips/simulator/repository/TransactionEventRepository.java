package com.dips.simulator.repository;

import com.dips.simulator.domain.TransactionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionEventRepository extends JpaRepository<TransactionEventEntity, Long> {
    List<TransactionEventEntity> findByTransactionIdOrderByIdAsc(UUID transactionId);
}

