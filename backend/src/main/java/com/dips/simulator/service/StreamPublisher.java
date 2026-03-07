package com.dips.simulator.service;

import com.dips.simulator.dto.StreamEventMessage;
import com.dips.simulator.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;

@Service
public class StreamPublisher {

    private final WebSocketSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public StreamPublisher(WebSocketSessionRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public void publishTransaction(String transactionId, StreamEventMessage message) {
        sendToSessions(registry.getTransactionSessions(transactionId), message);
    }

    public void publishUser(String userId, StreamEventMessage message) {
        sendToSessions(registry.getUserSessions(userId), message);
    }

    private void sendToSessions(Set<WebSocketSession> sessions, StreamEventMessage message) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException ignored) {
            }
        }
    }
}

