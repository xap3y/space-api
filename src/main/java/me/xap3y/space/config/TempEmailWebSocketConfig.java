package me.xap3y.space.config;

import me.xap3y.space.handler.TempEmailWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class TempEmailWebSocketConfig implements WebSocketConfigurer {

    private final TempEmailWebSocketHandler tempEmailWebSocketHandler;

    public TempEmailWebSocketConfig(TempEmailWebSocketHandler tempEmailWebSocketHandler) {
        this.tempEmailWebSocketHandler = tempEmailWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tempEmailWebSocketHandler, "/ws/email")
                .setAllowedOrigins("http://localhost:3000", "https://space.xap3y.fun", "https://xap3y.space", "http://localhost:3006");
    }
}
