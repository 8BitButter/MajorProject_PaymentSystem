package com.dips.simulator.service;

import com.dips.simulator.domain.enums.TransactionState;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class TransactionFsmService {

    private final Map<TransactionState, Set<TransactionState>> allowed = new EnumMap<>(TransactionState.class);

    public TransactionFsmService() {
        allowed.put(TransactionState.CREATED, EnumSet.of(
                TransactionState.OFFLINE_QUEUED,
                TransactionState.VALIDATION_PASSED,
                TransactionState.FAILED_PRE_DEBIT));
        allowed.put(TransactionState.OFFLINE_QUEUED, EnumSet.of(TransactionState.OFFLINE_RECEIVED));
        allowed.put(TransactionState.OFFLINE_RECEIVED, EnumSet.of(TransactionState.OFFLINE_DECRYPTED, TransactionState.FAILED_PRE_DEBIT));
        allowed.put(TransactionState.OFFLINE_DECRYPTED, EnumSet.of(TransactionState.VALIDATION_PASSED, TransactionState.FAILED_PRE_DEBIT));
        allowed.put(TransactionState.VALIDATION_PASSED, EnumSet.of(TransactionState.ROUTED_TO_SWITCH, TransactionState.FAILED_PRE_DEBIT));
        allowed.put(TransactionState.ROUTED_TO_SWITCH, EnumSet.of(TransactionState.DEBIT_REQUESTED, TransactionState.FAILED_PRE_DEBIT));
        allowed.put(TransactionState.DEBIT_REQUESTED, EnumSet.of(TransactionState.DEBIT_FAILED, TransactionState.DEBIT_SUCCESS));
        allowed.put(TransactionState.DEBIT_FAILED, EnumSet.of(TransactionState.FAILED_PRE_DEBIT));
        allowed.put(TransactionState.DEBIT_SUCCESS, EnumSet.of(TransactionState.CREDIT_REQUESTED));
        allowed.put(TransactionState.CREDIT_REQUESTED, EnumSet.of(TransactionState.CREDIT_FAILED, TransactionState.COMPLETED));
        allowed.put(TransactionState.CREDIT_FAILED, EnumSet.of(TransactionState.REVERSAL_REQUESTED));
        allowed.put(TransactionState.REVERSAL_REQUESTED, EnumSet.of(TransactionState.REVERSED));
    }

    public void validateTransition(TransactionState from, TransactionState to) {
        if (from == to) {
            return;
        }
        if (from.isTerminal()) {
            throw new DomainException("Terminal state cannot transition: " + from);
        }
        Set<TransactionState> next = allowed.get(from);
        if (next == null || !next.contains(to)) {
            throw new DomainException("Illegal state transition " + from + " -> " + to);
        }
    }
}

