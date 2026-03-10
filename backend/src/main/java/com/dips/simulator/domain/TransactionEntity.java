package com.dips.simulator.domain;

import com.dips.simulator.domain.enums.TransactionSource;
import com.dips.simulator.domain.enums.TransactionState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    private UUID id;

    @Column(name = "client_request_id", nullable = false, length = 128)
    private String clientRequestId;

    @Column(name = "correlation_id", nullable = false, length = 128)
    private String correlationId;

    @Column(name = "payer_vpa", nullable = false, length = 128)
    private String payerVpa;

    @Column(name = "payee_vpa", nullable = false, length = 128)
    private String payeeVpa;

    @Column(name = "payer_mpin", nullable = false, length = 16)
    private String payerMpin;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private TransactionState state;

    @Column(nullable = false)
    private boolean terminal;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getPayerVpa() {
        return payerVpa;
    }

    public void setPayerVpa(String payerVpa) {
        this.payerVpa = payerVpa;
    }

    public String getPayeeVpa() {
        return payeeVpa;
    }

    public void setPayeeVpa(String payeeVpa) {
        this.payeeVpa = payeeVpa;
    }

    public String getPayerMpin() {
        return payerMpin;
    }

    public void setPayerMpin(String payerMpin) {
        this.payerMpin = payerMpin;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionSource getSource() {
        return source;
    }

    public void setSource(TransactionSource source) {
        this.source = source;
    }

    public TransactionState getState() {
        return state;
    }

    public void setState(TransactionState state) {
        this.state = state;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
