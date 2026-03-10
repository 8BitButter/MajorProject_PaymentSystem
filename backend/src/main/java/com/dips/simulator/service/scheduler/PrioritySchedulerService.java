package com.dips.simulator.service.scheduler;

import com.dips.simulator.config.SchedulerProperties;
import com.dips.simulator.domain.enums.LoadProfile;
import com.dips.simulator.service.LoadProfileService;
import com.dips.simulator.service.PaymentExecutionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class PrioritySchedulerService {

    private final BlockingQueue<UUID> highQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<UUID> normalQueue = new LinkedBlockingQueue<>();
    private final SchedulerProperties schedulerProperties;
    private final LoadProfileService loadProfileService;
    private final PaymentExecutionService paymentExecutionService;
    private final boolean enabled;
    private int highBurstCounter = 0;

    public PrioritySchedulerService(
            SchedulerProperties schedulerProperties,
            LoadProfileService loadProfileService,
            PaymentExecutionService paymentExecutionService,
            @Value("${dips.scheduler.enabled:true}") boolean enabled
    ) {
        this.schedulerProperties = schedulerProperties;
        this.loadProfileService = loadProfileService;
        this.paymentExecutionService = paymentExecutionService;
        this.enabled = enabled;
    }

    public void enqueue(UUID transactionId, BigDecimal amount) {
        if (amount.compareTo(schedulerProperties.getSmallValueThreshold()) <= 0) {
            highQueue.add(transactionId);
        } else {
            normalQueue.add(transactionId);
        }
    }

    @Scheduled(fixedDelay = 120)
    public void processOne() {
        if (!enabled) {
            return;
        }
        UUID next = pickNext();
        if (next == null) {
            return;
        }
        paymentExecutionService.execute(next);
    }

    private UUID pickNext() {
        LoadProfile profile = loadProfileService.getProfile();
        if (profile == LoadProfile.HIGH_LOAD) {
            if (!highQueue.isEmpty() && highBurstCounter < schedulerProperties.getHighBurstBeforeNormal()) {
                highBurstCounter++;
                return highQueue.poll();
            }
            if (!normalQueue.isEmpty()) {
                highBurstCounter = 0;
                return normalQueue.poll();
            }
            return highQueue.poll();
        }
        if (!highQueue.isEmpty() && highBurstCounter < schedulerProperties.getHighBurstBeforeNormal()) {
            highBurstCounter++;
            return highQueue.poll();
        }
        if (!normalQueue.isEmpty()) {
            highBurstCounter = 0;
            return normalQueue.poll();
        }
        return highQueue.poll();
    }
}
