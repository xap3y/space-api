package me.xap3y.space.config;

import me.xap3y.space.handler.ReportPortalWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class ReportPortalWebSocketConfig implements WebSocketConfigurer {

    private final ReportPortalWebSocketHandler handler;

    public ReportPortalWebSocketConfig(ReportPortalWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/mc-reports")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:3006",
                        "http://192.168.100.100:3000",
                        "https://space.xap3y.eu"
                );
    }

    @Bean
    public ServletServerContainerFactoryBean reportPortalWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(256 * 1024);
        container.setMaxBinaryMessageBufferSize(64 * 1024);
        return container;
    }
}
