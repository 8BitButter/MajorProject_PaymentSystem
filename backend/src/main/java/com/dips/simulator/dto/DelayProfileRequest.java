package com.dips.simulator.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DelayProfileRequest {

    @NotNull
    @Min(0)
    private Long baseDelayMs;

    @NotNull
    @Min(0)
    private Long jitterMs;

    public Long getBaseDelayMs() {
        return baseDelayMs;
    }

    public void setBaseDelayMs(Long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
    }

    public Long getJitterMs() {
        return jitterMs;
    }

    public void setJitterMs(Long jitterMs) {
        this.jitterMs = jitterMs;
    }
}

