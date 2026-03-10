package com.dips.simulator.service;

import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.dto.DelayProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class StageDelayService {

    private final AtomicLong baseDelayMs;
    private final AtomicLong jitterMs;

    public StageDelayService(
            @Value("${dips.delay.base-ms:350}") long baseDelayMs,
            @Value("${dips.delay.jitter-ms:250}") long jitterMs
    ) {
        this.baseDelayMs = new AtomicLong(Math.max(0, baseDelayMs));
        this.jitterMs = new AtomicLong(Math.max(0, jitterMs));
    }

    public DelayProfileResponse getProfile() {
        return new DelayProfileResponse(baseDelayMs.get(), jitterMs.get());
    }

    public DelayProfileResponse updateProfile(long base, long jitter) {
        baseDelayMs.set(Math.max(0, base));
        jitterMs.set(Math.max(0, jitter));
        return getProfile();
    }

    public long computeDelayMs(UUID transactionId, PaymentStage stage) {
        long jitter = jitterMs.get();
        if (jitter <= 0) {
            return baseDelayMs.get();
        }
        int hash = (transactionId.toString() + "|" + stage.name()).hashCode();
        long deterministic = Math.floorMod(hash, jitter + 1);
        return baseDelayMs.get() + deterministic;
    }

    public long sleepFor(UUID transactionId, PaymentStage stage) {
        long delay = computeDelayMs(transactionId, stage);
        if (delay <= 0) {
            return 0;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return delay;
    }
}

