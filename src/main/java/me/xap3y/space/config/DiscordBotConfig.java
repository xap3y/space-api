package me.xap3y.space.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.service.DiscordBotService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class DiscordBotConfig {

    private final ServerInfo serverInfo;
    private final AtomicReference<GatewayDiscordClient> clientRef = new AtomicReference<>();

    public DiscordBotConfig(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
        // Initialize Discord bot in a separate thread to avoid blocking app startup
        initializeDiscordBotAsync();
    }

    private void initializeDiscordBotAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting Discord bot connection in background thread...");
                GatewayDiscordClient client = DiscordClientBuilder.create(serverInfo.getRemoteDiscordBotToken()).build()
                        .gateway()
                        .setEnabledIntents(IntentSet.of(
                                Intent.GUILD_MESSAGES,
                                Intent.MESSAGE_CONTENT,
                                Intent.GUILDS
                        ))
                        .setInitialPresence(ignore -> ClientPresence.online(ClientActivity.listening("to " +serverInfo.getFrontEndUrl())))
                        .login()
                        .block();
                clientRef.set(client);
                log.info("Discord bot connected successfully");
            } catch (Exception e) {
                log.error("Failed to connect Discord bot. App will continue without it.", e);
            }
        });
    }

    @Bean
    public Optional<GatewayDiscordClient> gatewayDiscordClient() {
        // Return Optional wrapping the client reference
        // The bean will return an empty Optional if connection is still pending or failed
        int maxWaitMs = 10000; // Wait max 10 seconds for bot to connect
        long startTime = System.currentTimeMillis();
        
        while (clientRef.get() == null && System.currentTimeMillis() - startTime < maxWaitMs) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for Discord bot to connect", e);
                return Optional.empty();
            }
        }
        
        if (clientRef.get() == null) {
            log.warn("Discord bot did not connect within timeout. Returning empty Optional - app will continue without Discord bot.");
            return Optional.empty();
        }
        
        return Optional.of(clientRef.get());
    }

    @Bean
    public Optional<RestClient> discordRestClient(Optional<GatewayDiscordClient> client) {
        return client.map(GatewayDiscordClient::getRestClient);
    }

    @Bean
    public long guildId() {
        return serverInfo.getRemoteDiscordBotGuildId();
    }
}