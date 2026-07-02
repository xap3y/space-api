package me.xap3y.space.config;

import me.xap3y.space.handler.BlackJackWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

@Configuration
@EnableWebSocket
public class BlackJackWebSocketConfig implements WebSocketConfigurer {

    private final BlackJackWebSocketHandler blackJackWebSocketHandler;

    public BlackJackWebSocketConfig(BlackJackWebSocketHandler blackJackWebSocketHandler) {
        this.blackJackWebSocketHandler = blackJackWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry registry) {
        registry.addHandler(blackJackWebSocketHandler, "/v1/minigame/blackjack/ws").setAllowedOrigins("*");
    }
}
