package com.dips.simulator.service;

import com.dips.simulator.domain.IdempotencyRecordEntity;
import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.dto.PushPaymentRequest;
import com.dips.simulator.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public Optional<IdempotencyRecordEntity> find(String payerVpa, String clientRequestId) {
        return repository.findByPayerVpaAndClientRequestId(payerVpa, clientRequestId);
    }

    @Transactional
    public void store(PushPaymentRequest request, TransactionEntity tx) {
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setPayerVpa(request.getPayerVpa());
        record.setClientRequestId(request.getClientRequestId());
        record.setTransactionId(tx.getId());
        record.setRequestHash(buildHash(request));
        record.setCreatedAt(OffsetDateTime.now());
        record.setExpiresAt(OffsetDateTime.now().plusHours(24));
        repository.save(record);
    }

    public void ensureSameRequest(IdempotencyRecordEntity existing, PushPaymentRequest request) {
        String hash = buildHash(request);
        if (!hash.equals(existing.getRequestHash())) {
            throw new DomainException("clientRequestId reused with different payload");
        }
    }

    private String buildHash(PushPaymentRequest request) {
        String payload = request.getClientRequestId() + "|" + request.getPayerVpa() + "|" + request.getPayeeVpa() + "|" + request.getAmount();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

