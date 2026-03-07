package com.dips.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "dips.scheduler")
public class SchedulerProperties {

    private BigDecimal smallValueThreshold = new BigDecimal("500.00");
    private long highLoadNormalPauseMs = 1200;
    private int highBurstBeforeNormal = 3;

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
}

