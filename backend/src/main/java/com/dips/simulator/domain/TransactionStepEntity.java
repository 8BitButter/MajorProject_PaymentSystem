package com.dips.simulator.domain;

import com.dips.simulator.domain.enums.PaymentStage;
import com.dips.simulator.domain.enums.StageBranch;
import com.dips.simulator.domain.enums.StageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_steps")
public class TransactionStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StageStatus status;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_stage", length = 32)
    private PaymentStage nextStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StageBranch branch;

    @Column(name = "processing_ms", nullable = false)
    private long processingMs;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private OffsetDateTime endedAt;

    @Column(name = "input_summary", length = 512)
    private String inputSummary;

    @Column(name = "output_summary", length = 512)
    private String outputSummary;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
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

    public StageBranch getBranch() {
        return branch;
    }

    public void setBranch(StageBranch branch) {
        this.branch = branch;
    }

    public long getProcessingMs() {
        return processingMs;
    }

    public void setProcessingMs(long processingMs) {
        this.processingMs = processingMs;
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

