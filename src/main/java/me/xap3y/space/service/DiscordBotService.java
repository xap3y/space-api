package me.xap3y.space.service;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class DiscordBotService {

    @Getter
    private final GatewayDiscordClient client;
    private final long guildId;
    private final ServerInfo serverInfo;

    public DiscordBotService(GatewayDiscordClient client, long guildId, ServerInfo serverInfo) {
        this.client = client;
        this.guildId = guildId;
        this.serverInfo = serverInfo;

        if (!serverInfo.getUseDiscordBot()) return;

        //registerEvents();
        log.info("GUILD_ID: {}", guildId);
    }



    /*private void registerEvents() {
        log.info("Registering Discord bot events...");
        client.on(MessageCreateEvent.class)
                .filter(event -> event.getGuildId().map(Snowflake::asLong).orElse(0L) == guildId)
                .subscribe(event -> {
                    String content = event.getMessage().getContent();
                    if (content.equalsIgnoreCase("!ping")) {
                        event.getMessage().getChannel().flatMap(channel -> channel.createMessage("Pong!")).subscribe();
                    }
                });

        client.on(ReadyEvent.class)
                .subscribe(event -> {
                    log.info("Discord bot is ready! Logged in as: {}", event.getSelf().getUsername());
                    sendMessage("Subscribe in <#" + serverInfo.getRemoteDiscordBotChannelId() + ">").subscribe();
                });
    }*/

    public Mono<Void> sendMessage(long channelId, String message) {
        return client.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(message))
                .then();
    }

    public Mono<Void> sendMessage(String message) {
        return sendMessage(serverInfo.getRemoteDiscordBotChannelId(), message);
    }

    public Mono<Void> editChannelTopic(long channelId, String topic) {
        return client.getChannelById(Snowflake.of(channelId))
                .ofType(TextChannel.class)
                .flatMap(channel -> channel.edit().withTopic(topic))
                .then();
    }

}
