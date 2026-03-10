package com.dips.simulator.dto;

public class DelayProfileResponse {

    private long baseDelayMs;
    private long jitterMs;

    public DelayProfileResponse() {
    }

    public DelayProfileResponse(long baseDelayMs, long jitterMs) {
        this.baseDelayMs = baseDelayMs;
        this.jitterMs = jitterMs;
    }

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public void setBaseDelayMs(long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
    }

    public long getJitterMs() {
        return jitterMs;
    }

    public void setJitterMs(long jitterMs) {
        this.jitterMs = jitterMs;
    }
}

