package com.dips.simulator.domain.enums;

public enum TransactionState {
    CREATED,
    OFFLINE_QUEUED,
    OFFLINE_RECEIVED,
    OFFLINE_DECRYPTED,
    VALIDATION_PASSED,
    ROUTED_TO_SWITCH,
    DEBIT_REQUESTED,
    DEBIT_FAILED,
    DEBIT_SUCCESS,
    CREDIT_REQUESTED,
    CREDIT_FAILED,
    REVERSAL_REQUESTED,
    REVERSED,
    COMPLETED,
    FAILED_PRE_DEBIT;

    public boolean isTerminal() {
        return this == COMPLETED || this == REVERSED || this == FAILED_PRE_DEBIT;
    }
}

