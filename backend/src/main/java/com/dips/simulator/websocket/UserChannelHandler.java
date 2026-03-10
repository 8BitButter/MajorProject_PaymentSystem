package com.dips.simulator.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class UserChannelHandler extends TextWebSocketHandler {

    private final WebSocketSessionRegistry registry;

    public UserChannelHandler(WebSocketSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractLastSegment(session);
        if (userId != null) {
            registry.registerUser(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = extractLastSegment(session);
        if (userId != null) {
            registry.unregisterUser(userId, session);
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
        if (parts.length == 0) {
            return null;
        }
        return URLDecoder.decode(parts[parts.length - 1], StandardCharsets.UTF_8);
    }
}
