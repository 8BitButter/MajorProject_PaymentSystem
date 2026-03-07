package com.dips.simulator.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private final Map<String, Set<WebSocketSession>> transactionSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void registerTransaction(String transactionId, WebSocketSession session) {
        transactionSessions.computeIfAbsent(transactionId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregisterTransaction(String transactionId, WebSocketSession session) {
        Set<WebSocketSession> sessions = transactionSessions.get(transactionId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    public void registerUser(String userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregisterUser(String userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    public Set<WebSocketSession> getTransactionSessions(String transactionId) {
        return transactionSessions.getOrDefault(transactionId, Set.of());
    }

    public Set<WebSocketSession> getUserSessions(String userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }
}

