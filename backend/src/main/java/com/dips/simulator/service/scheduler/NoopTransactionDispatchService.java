package com.dips.simulator.service.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@ConditionalOnProperty(value = "dips.scheduler.enabled", havingValue = "false")
public class NoopTransactionDispatchService implements TransactionDispatchService {

    @Override
    public void enqueue(UUID transactionId, BigDecimal amount) {
        // Intentionally no-op when scheduler is disabled (e.g., deterministic tests).
    }
}
