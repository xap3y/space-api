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

@Slf4j
@Configuration
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class DiscordBotConfig {

    private final ServerInfo serverInfo;

    public DiscordBotConfig(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create(serverInfo.getRemoteDiscordBotToken()).build()
                .gateway()
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILD_MESSAGES,
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILDS
                ))
                .setInitialPresence(ignore -> ClientPresence.online(ClientActivity.listening("to " +serverInfo.getFrontEndUrl())))
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }

    @Bean
    public long guildId() {
        return serverInfo.getRemoteDiscordBotGuildId();
    }
}