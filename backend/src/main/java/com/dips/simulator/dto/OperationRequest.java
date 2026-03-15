package com.dips.simulator.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class OperationRequest {

    private UUID transactionId;
    private String correlationId;
    private String accountId;
    private BigDecimal amount;

    public OperationRequest() {
    }

    public OperationRequest(UUID transactionId, String correlationId, String accountId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.correlationId = correlationId;
        this.accountId = accountId;
        this.amount = amount;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
