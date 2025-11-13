package me.xap3y.space.config;

import me.xap3y.space.handler.PlaycoreWebInSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

@Configuration
@EnableWebSocket
public class PlaycoreWebInWebSocketConfig implements WebSocketConfigurer {

    private final PlaycoreWebInSocketHandler playcoreWebInSocketHandler;

    public PlaycoreWebInWebSocketConfig(PlaycoreWebInSocketHandler playcoreWebInSocketHandler) {
        this.playcoreWebInSocketHandler = playcoreWebInSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry registry) {
        registry.addHandler(playcoreWebInSocketHandler, "/ws/playcore/in")
                .setAllowedOrigins("*");
    }
}
