package com.dips.simulator.dto;

import java.util.UUID;

public class OfflineSmsResponse {

    private boolean accepted;
    private String reason;
    private UUID transactionId;

    public OfflineSmsResponse() {
    }

    public OfflineSmsResponse(boolean accepted, String reason, UUID transactionId) {
        this.accepted = accepted;
        this.reason = reason;
        this.transactionId = transactionId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }
}

