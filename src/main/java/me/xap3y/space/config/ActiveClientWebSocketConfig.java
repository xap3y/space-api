package me.xap3y.space.config;

import me.xap3y.space.handler.ActiveClientWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ActiveClientWebSocketConfig implements WebSocketConfigurer {

    private final ActiveClientWebSocketHandler handler;

    public ActiveClientWebSocketConfig(ActiveClientWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(handler, "/ws/active")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:3006",
                        "https://space.xap3y.eu",
                        "http://192.168.100.100:3000"
                );
    }
}
