package me.xap3y.space.listener;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.service.DiscordBotService;
import me.xap3y.space.service.RemoteMessageService;
import me.xap3y.space.service.TelegramService;
import me.xap3y.space.service.WebhookService;
import me.xap3y.space.util.TelegramBot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static me.xap3y.space.SpaceApplication.*;

@Slf4j
@Component
public class ApplicationListener {

    private final WebhookService webhookService;
    private final TelegramService telegramService;
    private final ServerInfo serverInfo;
    private final RemoteMessageService remoteMessageService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    public ApplicationListener(WebhookService webhookService, TelegramService telegramService, ServerInfo serverInfo, RemoteMessageService remoteMessageService) {
        this.webhookService = webhookService;
        this.telegramService = telegramService;
        this.serverInfo = serverInfo;
        this.remoteMessageService = remoteMessageService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        log.debug("runAfterStartup()");
        webhookService.init();
        String botToken = serverInfo.getTelegramBotToken();
        TelegramBot telegramBot = new TelegramBot(botToken);
        telegramBot.init();

        if (serverInfo.getUseTelegramBot()) {
            try {
                botsApplication.registerBot(botToken, telegramBot);
                telegramService.setTelegramBot(telegramBot);
                telegramService.sendMessage("5759660343", "Space-API started at " + startedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " running on " + "ENV: *" + env + "* | VER: *" + VERSION + "*");
            } catch (TelegramApiException ex) {
                log.error("Telegram bot failed to registered!");
            }
        }

        log.info("INIT DIS - CHANNEL_ID: {}", serverInfo.getRemoteDiscordBotChannelId());
        //webhookService.postMessage(serverInfo.getHost());
        //remoteMessageService.sendDiscordBotMessage("subscribed to channelID " + serverInfo.getRemoteDiscordBotChannelId() + " <#" + serverInfo.getRemoteDiscordBotChannelId() + ">").subscribe();
        /*scheduler.scheduleAtFixedRate(
                this::runDiscordTask,
                3,
                30,
                TimeUnit.SECONDS
        );*/
    }

    private void runDiscordTask() {
        log.info("runDiscordTask() - Sending message to Discord channel: {}", serverInfo.getRemoteDiscordBotChannelId());
        remoteMessageService.updateChannelTopic(serverInfo.getRemoteDiscordBotChannelId(), "TEST").subscribe();
    }


}
