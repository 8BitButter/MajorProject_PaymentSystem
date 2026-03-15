package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.TransactionEventEntity;
import com.dips.simulator.domain.enums.TransactionState;
import com.dips.simulator.dto.StreamEventMessage;
import com.dips.simulator.repository.TransactionEventRepository;
import com.dips.simulator.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class TransactionStateService {

    private static final Logger log = LoggerFactory.getLogger(TransactionStateService.class);

    private final TransactionFsmService fsmService;
    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final StreamPublisher streamPublisher;

    public TransactionStateService(
            TransactionFsmService fsmService,
            TransactionRepository transactionRepository,
            TransactionEventRepository eventRepository,
            StreamPublisher streamPublisher
    ) {
        this.fsmService = fsmService;
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.streamPublisher = streamPublisher;
    }

    @Transactional
    public void recordInitial(TransactionEntity tx, String actor, String reason) {
        log.info("event=state_transition from=NULL to={} actor={} reason={}", tx.getState(), actor, reason);
        tx.setTerminal(tx.getState().isTerminal());
        tx.setUpdatedAt(OffsetDateTime.now());
        transactionRepository.save(tx);

        TransactionEventEntity event = new TransactionEventEntity();
        event.setTransactionId(tx.getId());
        event.setFromState(null);
        event.setToState(tx.getState().name());
        event.setActor(actor);
        event.setReason(reason);
        event.setCreatedAt(OffsetDateTime.now());
        eventRepository.save(event);

        StreamEventMessage message = new StreamEventMessage();
        message.setTransactionId(tx.getId());
        message.setFromState(null);
        message.setToState(tx.getState().name());
        message.setActor(actor);
        message.setReason(reason);
        message.setCreatedAt(event.getCreatedAt());

        streamPublisher.publishTransaction(tx.getId().toString(), message);
        streamPublisher.publishUser(tx.getPayerVpa(), message);
        streamPublisher.publishUser(tx.getPayeeVpa(), message);
    }

    @Transactional
    public void transition(TransactionEntity tx, TransactionState to, String actor, String reason) {
        TransactionState from = tx.getState();
        log.info("event=state_transition from={} to={} actor={} reason={}", from, to, actor, reason);
        fsmService.validateTransition(from, to);
        tx.setState(to);
        tx.setTerminal(to.isTerminal());
        tx.setUpdatedAt(OffsetDateTime.now());
        transactionRepository.save(tx);

        TransactionEventEntity event = new TransactionEventEntity();
        event.setTransactionId(tx.getId());
        event.setFromState(from == null ? null : from.name());
        event.setToState(to.name());
        event.setActor(actor);
        event.setReason(reason);
        event.setCreatedAt(OffsetDateTime.now());
        eventRepository.save(event);

        StreamEventMessage message = new StreamEventMessage();
        message.setTransactionId(tx.getId());
        message.setFromState(from == null ? null : from.name());
        message.setToState(to.name());
        message.setActor(actor);
        message.setReason(reason);
        message.setCreatedAt(event.getCreatedAt());

        streamPublisher.publishTransaction(tx.getId().toString(), message);
        streamPublisher.publishUser(tx.getPayerVpa(), message);
        streamPublisher.publishUser(tx.getPayeeVpa(), message);
    }
}
