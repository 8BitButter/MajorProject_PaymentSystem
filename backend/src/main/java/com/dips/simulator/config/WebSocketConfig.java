package com.dips.simulator.config;

import com.dips.simulator.websocket.TransactionChannelHandler;
import com.dips.simulator.websocket.UserChannelHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TransactionChannelHandler transactionHandler;
    private final UserChannelHandler userHandler;

    public WebSocketConfig(TransactionChannelHandler transactionHandler, UserChannelHandler userHandler) {
        this.transactionHandler = transactionHandler;
        this.userHandler = userHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(transactionHandler, "/ws/transactions/*").setAllowedOrigins("*");
        registry.addHandler(userHandler, "/ws/users/*").setAllowedOrigins("*");
    }
}

