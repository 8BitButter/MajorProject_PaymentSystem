package com.dips.simulator.repository;

import com.dips.simulator.domain.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    long countByCreatedAtAfter(OffsetDateTime createdAfter);

    Page<TransactionEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<TransactionEntity> findByPayerVpaOrPayeeVpaOrderByCreatedAtDesc(String payerVpa, String payeeVpa, Pageable pageable);
}

