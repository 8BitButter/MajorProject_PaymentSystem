package com.dips.simulator.service.crypto;

import com.dips.simulator.config.SmsProperties;
import com.dips.simulator.dto.OfflineSmsDecryptedPayload;
import com.dips.simulator.dto.OfflineSmsRequest;
import com.dips.simulator.service.DomainException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class SmsCryptoService {

    private static final int GCM_TAG_BITS = 128;
    private final SmsProperties smsProperties;
    private final ObjectMapper objectMapper;

    public SmsCryptoService(SmsProperties smsProperties, ObjectMapper objectMapper) {
        this.smsProperties = smsProperties;
        this.objectMapper = objectMapper;
    }

    public OfflineSmsDecryptedPayload decryptPayload(OfflineSmsRequest request) {
        validateTimestamp(request.getTimestampEpochSeconds());
        try {
            byte[] key = Base64.getDecoder().decode(smsProperties.getAesKeyBase64());
            byte[] iv = Base64.getDecoder().decode(request.getIvBase64());
            byte[] cipherText = Base64.getDecoder().decode(request.getCipherTextBase64());

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcm = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcm);
            cipher.updateAAD(request.getMessageId().getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(cipherText);
            return objectMapper.readValue(plain, OfflineSmsDecryptedPayload.class);
        } catch (Exception ex) {
            throw new DomainException("Invalid encrypted SMS payload");
        }
    }

    private void validateTimestamp(long epochSeconds) {
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - epochSeconds) > smsProperties.getMaxSkewSeconds()) {
            throw new DomainException("SMS payload timestamp skew too large");
        }
    }
}

