package me.xap3y.space.listener;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.service.*;
import me.xap3y.space.util.TelegramBot;
import me.xap3y.space.util.TelegramVerifyBot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
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
    private final Optional<RemoteMessageService> remoteMessageService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final TelegramVerifyService telegramVerifyService;
    private final EmailVerifyCodeService emailVerifyCodeService;
    private final TelegramConnectionService telegramConnectionService;
    private final TelegramVerifyBot telegramVerifyBot;
    private final ImageService imageService;


    public ApplicationListener(WebhookService webhookService, TelegramService telegramService, ServerInfo serverInfo, Optional<RemoteMessageService> remoteMessageService, TelegramVerifyService telegramVerifyService, EmailVerifyCodeService emailVerifyCodeService, TelegramConnectionService telegramConnectionService, TelegramVerifyBot telegramVerifyBot, ImageService imageService) {
        this.webhookService = webhookService;
        this.telegramService = telegramService;
        this.serverInfo = serverInfo;
        this.remoteMessageService = remoteMessageService;
        this.telegramVerifyService = telegramVerifyService;
        this.emailVerifyCodeService = emailVerifyCodeService;
        this.telegramConnectionService = telegramConnectionService;
        this.telegramVerifyBot = telegramVerifyBot;
        this.imageService = imageService;
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

        String verifyBotToken = serverInfo.getTelegramVerifyBotToken();
        //TelegramVerifyBot telegramVerifyBot = new TelegramVerifyBot(verifyBotToken, emailVerifyCodeService, telegramConnectionService);
        //telegramVerifyBot.init();

        if (serverInfo.getUseTelegramVerifyBot()) {
            try {
                log.info("Telegram Verify Bot registered successfully.");
                botsApplication.registerBot(serverInfo.getTelegramVerifyBotToken(), telegramVerifyBot);
            } catch (TelegramApiException ex) {
                log.error("Telegram Verify Bot failed to register!", ex);
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

        imageService.fixMissingVideoPostersAsync();
    }

    private void runDiscordTask() {
        log.info("runDiscordTask() - Sending message to Discord channel: {}", serverInfo.getRemoteDiscordBotChannelId());
        if (remoteMessageService.isPresent()) {
            remoteMessageService.get().updateChannelTopic(serverInfo.getRemoteDiscordBotChannelId(), "TEST").subscribe();
        } else {
            log.warn("Discord bot service not available, skipping Discord task");
        }
    }


}
