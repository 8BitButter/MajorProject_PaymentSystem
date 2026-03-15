package com.dips.simulator.repository;

import com.dips.simulator.domain.LedgerEntryEntity;
import com.dips.simulator.domain.enums.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {
    boolean existsByTransactionIdAndEntryType(UUID transactionId, LedgerEntryType entryType);
}

