package com.dips.simulator.domain;

import com.dips.simulator.domain.enums.BankOperationType;
import com.dips.simulator.domain.enums.ProcessedOperationResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "processed_operations",
        uniqueConstraints = @UniqueConstraint(name = "uq_processed_operations_tx_operation", columnNames = {"transaction_id", "operation_type"})
)
public class ProcessedOperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 32)
    private BankOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProcessedOperationResult result;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public BankOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(BankOperationType operationType) {
        this.operationType = operationType;
    }

    public ProcessedOperationResult getResult() {
        return result;
    }

    public void setResult(ProcessedOperationResult result) {
        this.result = result;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
