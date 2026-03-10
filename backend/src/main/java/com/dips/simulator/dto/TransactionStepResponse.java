package com.dips.simulator.dto;

import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.domain.enums.StageBranch;
import com.dips.simulator.domain.enums.StageStatus;

import java.time.OffsetDateTime;

public class TransactionStepResponse {

    private Long id;
    private PaymentStage stage;
    private StageStatus status;
    private String actor;
    private String reason;
    private PaymentStage nextStage;
    private long processingMs;
    private StageBranch branch;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private String inputSummary;
    private String outputSummary;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentStage getStage() {
        return stage;
    }

    public void setStage(PaymentStage stage) {
        this.stage = stage;
    }

    public StageStatus getStatus() {
        return status;
    }

    public void setStatus(StageStatus status) {
        this.status = status;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public PaymentStage getNextStage() {
        return nextStage;
    }

    public void setNextStage(PaymentStage nextStage) {
        this.nextStage = nextStage;
    }

    public long getProcessingMs() {
        return processingMs;
    }

    public void setProcessingMs(long processingMs) {
        this.processingMs = processingMs;
    }

    public StageBranch getBranch() {
        return branch;
    }

    public void setBranch(StageBranch branch) {
        this.branch = branch;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }
}

