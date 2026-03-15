package com.dips.simulator.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BankOperationStatusResponse {

    private UUID transactionId;
    private String operation;
    private String status;
    private String reasonCode;
    private String message;
    private OffsetDateTime processedAt;

    public BankOperationStatusResponse() {
    }

    public BankOperationStatusResponse(
            UUID transactionId,
            String operation,
            String status,
            String reasonCode,
            String message,
            OffsetDateTime processedAt
    ) {
        this.transactionId = transactionId;
        this.operation = operation;
        this.status = status;
        this.reasonCode = reasonCode;
        this.message = message;
        this.processedAt = processedAt;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
