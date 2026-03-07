package com.dips.simulator.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TransactionChannelHandler extends TextWebSocketHandler {

    private final WebSocketSessionRegistry registry;

    public TransactionChannelHandler(WebSocketSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String txId = extractLastSegment(session);
        if (txId != null) {
            registry.registerTransaction(txId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String txId = extractLastSegment(session);
        if (txId != null) {
            registry.unregisterTransaction(txId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Server push only.
    }

    private String extractLastSegment(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts.length == 0 ? null : parts[parts.length - 1];
    }
}

