package com.dips.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "dips.scheduler")
public class SchedulerProperties {

    private BigDecimal smallValueThreshold = new BigDecimal("500.00");
    private long highLoadNormalPauseMs = 1200;
    private int highBurstBeforeNormal = 3;
    private long queuePollDelayMs = 120;
    private int maxRetryAttempts = 8;
    private long operationTimeoutMs = 3000;
    private int operationMaxRetries = 2;
    private long operationRetryBackoffMs = 1000;

    public BigDecimal getSmallValueThreshold() {
        return smallValueThreshold;
    }

    public void setSmallValueThreshold(BigDecimal smallValueThreshold) {
        this.smallValueThreshold = smallValueThreshold;
    }

    public long getHighLoadNormalPauseMs() {
        return highLoadNormalPauseMs;
    }

    public void setHighLoadNormalPauseMs(long highLoadNormalPauseMs) {
        this.highLoadNormalPauseMs = highLoadNormalPauseMs;
    }

    public int getHighBurstBeforeNormal() {
        return highBurstBeforeNormal;
    }

    public void setHighBurstBeforeNormal(int highBurstBeforeNormal) {
        this.highBurstBeforeNormal = highBurstBeforeNormal;
    }

    public long getQueuePollDelayMs() {
        return queuePollDelayMs;
    }

    public void setQueuePollDelayMs(long queuePollDelayMs) {
        this.queuePollDelayMs = queuePollDelayMs;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public long getOperationTimeoutMs() {
        return operationTimeoutMs;
    }

    public void setOperationTimeoutMs(long operationTimeoutMs) {
        this.operationTimeoutMs = operationTimeoutMs;
    }

    public int getOperationMaxRetries() {
        return operationMaxRetries;
    }

    public void setOperationMaxRetries(int operationMaxRetries) {
        this.operationMaxRetries = operationMaxRetries;
    }

    public long getOperationRetryBackoffMs() {
        return operationRetryBackoffMs;
    }

    public void setOperationRetryBackoffMs(long operationRetryBackoffMs) {
        this.operationRetryBackoffMs = operationRetryBackoffMs;
    }
}

