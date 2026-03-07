package com.dips.simulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OfflineSmsRequest {

    @NotBlank
    private String messageId;

    @NotBlank
    private String ivBase64;

    @NotBlank
    private String cipherTextBase64;

    @NotNull
    private Long timestampEpochSeconds;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getIvBase64() {
        return ivBase64;
    }

    public void setIvBase64(String ivBase64) {
        this.ivBase64 = ivBase64;
    }

    public String getCipherTextBase64() {
        return cipherTextBase64;
    }

    public void setCipherTextBase64(String cipherTextBase64) {
        this.cipherTextBase64 = cipherTextBase64;
    }

    public Long getTimestampEpochSeconds() {
        return timestampEpochSeconds;
    }

    public void setTimestampEpochSeconds(Long timestampEpochSeconds) {
        this.timestampEpochSeconds = timestampEpochSeconds;
    }
}

