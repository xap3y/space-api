package me.xap3y.space.config;

import me.xap3y.space.handler.VerifyWebSocketHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class VerifyWebSocketConfig implements WebSocketConfigurer {

    private final VerifyWebSocketHandler verifyWebSocketHandler;

    public VerifyWebSocketConfig(VerifyWebSocketHandler handler) {
        this.verifyWebSocketHandler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(verifyWebSocketHandler, "/ws/verify")
                .setAllowedOrigins("http://localhost:3000", "https://space.xap3y.fun", "https://xap3y.space", "http://localhost:3006");
        // .withSockJS(); // (NOT needed if using native WebSocket)
    }
}
