package com.dips.simulator.service;

import com.dips.simulator.domain.SmsMessageLogEntity;
import com.dips.simulator.dto.OfflineSmsDecryptedPayload;
import com.dips.simulator.dto.OfflineSmsRequest;
import com.dips.simulator.dto.OfflineSmsResponse;
import com.dips.simulator.dto.PushPaymentRequest;
import com.dips.simulator.service.crypto.SmsCryptoService;
import com.dips.simulator.repository.SmsMessageLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class OfflineSmsService {

    private final SmsMessageLogRepository smsLogRepository;
    private final SmsCryptoService smsCryptoService;
    private final PaymentService paymentService;

    public OfflineSmsService(
            SmsMessageLogRepository smsLogRepository,
            SmsCryptoService smsCryptoService,
            PaymentService paymentService
    ) {
        this.smsLogRepository = smsLogRepository;
        this.smsCryptoService = smsCryptoService;
        this.paymentService = paymentService;
    }

    @Transactional
    public OfflineSmsResponse handle(OfflineSmsRequest request) {
        if (smsLogRepository.findByMessageId(request.getMessageId()).isPresent()) {
            return new OfflineSmsResponse(false, "Duplicate messageId", null);
        }
        try {
            OfflineSmsDecryptedPayload payload = smsCryptoService.decryptPayload(request);
            PushPaymentRequest push = new PushPaymentRequest();
            push.setClientRequestId(payload.getClientRequestId());
            push.setPayerVpa(payload.getPayerVpa());
            push.setPayeeVpa(payload.getPayeeVpa());
            push.setAmount(payload.getAmount());
            push.setMpin(payload.getMpin());
            var response = paymentService.initiateOfflinePush(push);
            saveLog(request.getMessageId(), true, "Accepted");
            return new OfflineSmsResponse(true, "Accepted", response.getTransactionId());
        } catch (Exception ex) {
            saveLog(request.getMessageId(), false, ex.getMessage());
            if (ex instanceof DomainException domainEx) {
                return new OfflineSmsResponse(false, domainEx.getMessage(), null);
            }
            return new OfflineSmsResponse(false, "Invalid SMS payload", null);
        }
    }

    private void saveLog(String messageId, boolean accepted, String reason) {
        SmsMessageLogEntity log = new SmsMessageLogEntity();
        log.setMessageId(messageId);
        log.setAccepted(accepted);
        log.setReason(reason);
        log.setCreatedAt(OffsetDateTime.now());
        smsLogRepository.save(log);
    }
}

