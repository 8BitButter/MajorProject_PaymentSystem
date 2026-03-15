package com.dips.simulator.service.scheduler;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionDispatchService {
    void enqueue(UUID transactionId, BigDecimal amount);
}

