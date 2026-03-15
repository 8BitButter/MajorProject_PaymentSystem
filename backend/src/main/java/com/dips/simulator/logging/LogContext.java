package com.dips.simulator.logging;

import org.slf4j.MDC;

import java.util.UUID;

public final class LogContext {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String TRANSACTION_ID_KEY = "transactionId";

    private LogContext() {
    }

    public static void setCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return;
        }
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    public static String ensureCorrelationId(String fallbackPrefix) {
        String existing = getCorrelationId();
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String generated = fallbackPrefix + "-" + UUID.randomUUID();
        setCorrelationId(generated);
        return generated;
    }

    public static void clearCorrelationId() {
        MDC.remove(CORRELATION_ID_KEY);
    }

    public static void setTransactionId(UUID transactionId) {
        if (transactionId == null) {
            return;
        }
        MDC.put(TRANSACTION_ID_KEY, transactionId.toString());
    }

    public static void clearTransactionId() {
        MDC.remove(TRANSACTION_ID_KEY);
    }
}
