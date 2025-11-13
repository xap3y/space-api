package me.xap3y.space.config;

import me.xap3y.space.handler.PlaycoreWebOutSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class PlaycoreWebOutWebSocketConfig implements WebSocketConfigurer {

    private final PlaycoreWebOutSocketHandler playcoreWebOutSocketHandler;

    public PlaycoreWebOutWebSocketConfig(PlaycoreWebOutSocketHandler playcoreWebOutSocketHandler) {
        this.playcoreWebOutSocketHandler = playcoreWebOutSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(playcoreWebOutSocketHandler, "/ws/playcore/out")
                .setAllowedOrigins("*");
    }
}
