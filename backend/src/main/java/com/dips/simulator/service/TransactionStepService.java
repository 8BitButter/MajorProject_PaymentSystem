package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.TransactionStepEntity;
import com.dips.simulator.dto.StreamEventMessage;
import com.dips.simulator.dto.TransactionStepResponse;
import com.dips.simulator.repository.TransactionStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionStepService {

    private final TransactionStepRepository stepRepository;
    private final StreamPublisher streamPublisher;

    public TransactionStepService(TransactionStepRepository stepRepository, StreamPublisher streamPublisher) {
        this.stepRepository = stepRepository;
        this.streamPublisher = streamPublisher;
    }

    @Transactional
    public TransactionStepEntity record(
            TransactionEntity tx,
            com.dips.simulator.dto.StageDecision decision,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            String inputSummary,
            String outputSummary
    ) {
        TransactionStepEntity step = new TransactionStepEntity();
        step.setTransactionId(tx.getId());
        step.setStage(decision.getStage());
        step.setStatus(decision.getStatus());
        step.setActor(decision.getActor());
        step.setReason(decision.getReason());
        step.setNextStage(decision.getNextStage());
        step.setBranch(decision.getBranch());
        step.setProcessingMs(decision.getProcessingMs());
        step.setStartedAt(startedAt);
        step.setEndedAt(endedAt);
        step.setInputSummary(inputSummary);
        step.setOutputSummary(outputSummary);
        TransactionStepEntity saved = stepRepository.save(step);

        StreamEventMessage message = new StreamEventMessage();
        message.setEventType("STAGE_DECISION");
        message.setStepId(saved.getId());
        message.setTransactionId(tx.getId());
        message.setStage(saved.getStage());
        message.setStatus(saved.getStatus());
        message.setActor(saved.getActor());
        message.setReason(saved.getReason());
        message.setNextStage(saved.getNextStage());
        message.setProcessingMs(saved.getProcessingMs());
        message.setBranch(saved.getBranch());
        message.setStartedAt(saved.getStartedAt());
        message.setEndedAt(saved.getEndedAt());
        message.setCreatedAt(saved.getEndedAt());

        streamPublisher.publishTransaction(tx.getId().toString(), message);
        streamPublisher.publishUser(tx.getPayerVpa(), message);
        streamPublisher.publishUser(tx.getPayeeVpa(), message);
        return saved;
    }

    public List<TransactionStepResponse> getTimeline(UUID transactionId) {
        return stepRepository.findByTransactionIdOrderByIdAsc(transactionId).stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionStepResponse toResponse(TransactionStepEntity step) {
        TransactionStepResponse response = new TransactionStepResponse();
        response.setId(step.getId());
        response.setStage(step.getStage());
        response.setStatus(step.getStatus());
        response.setActor(step.getActor());
        response.setReason(step.getReason());
        response.setNextStage(step.getNextStage());
        response.setProcessingMs(step.getProcessingMs());
        response.setBranch(step.getBranch());
        response.setStartedAt(step.getStartedAt());
        response.setEndedAt(step.getEndedAt());
        response.setInputSummary(step.getInputSummary());
        response.setOutputSummary(step.getOutputSummary());
        return response;
    }
}

