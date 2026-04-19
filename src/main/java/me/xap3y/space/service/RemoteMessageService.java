package me.xap3y.space.service;

import discord4j.core.GatewayDiscordClient;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class RemoteMessageService {

    private final Optional<DiscordBotService> bot;
    private final ServerInfo serverInfo;

    public RemoteMessageService(Optional<DiscordBotService> bot, ServerInfo serverInfo) {
        this.bot = bot;
        this.serverInfo = serverInfo;
    }

    public Mono<Void> sendDiscordBotMessage(long channelId, String content) {
        if (bot.isPresent()) {
            return bot.get().sendMessage(channelId, content);
        } else {
            return Mono.empty();
        }
    }

    public Mono<Void> sendDiscordBotMessage(String content) {
        return sendDiscordBotMessage(serverInfo.getRemoteDiscordBotChannelId(), content);
    }

    public Mono<Void> updateChannelTopic(long channelId, String topic) {
        if (bot.isPresent()) {
            return bot.get().editChannelTopic(channelId, topic);
        } else {
            return Mono.empty();
        }
    }

    public Optional<GatewayDiscordClient> getClient() {
        return bot.flatMap(DiscordBotService::getClient);
    }
}
