package com.dips.simulator.dto;

import com.dips.simulator.domain.enums.TransactionState;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PushPaymentResponse {

    private UUID transactionId;
    private boolean idempotentReplay;
    private TransactionState state;
    private OffsetDateTime acceptedAt;

    public PushPaymentResponse() {
    }

    public PushPaymentResponse(UUID transactionId, boolean idempotentReplay, TransactionState state, OffsetDateTime acceptedAt) {
        this.transactionId = transactionId;
        this.idempotentReplay = idempotentReplay;
        this.state = state;
        this.acceptedAt = acceptedAt;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isIdempotentReplay() {
        return idempotentReplay;
    }

    public void setIdempotentReplay(boolean idempotentReplay) {
        this.idempotentReplay = idempotentReplay;
    }

    public TransactionState getState() {
        return state;
    }

    public void setState(TransactionState state) {
        this.state = state;
    }

    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(OffsetDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}

