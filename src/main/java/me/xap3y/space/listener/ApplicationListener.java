package me.xap3y.space.listener;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.service.*;
import me.xap3y.space.util.TelegramBot;
import me.xap3y.space.util.TelegramVerifyBot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static me.xap3y.space.SpaceApplication.*;

@Slf4j
@Component
@Profile("!local")
public class ApplicationListener {

    private final WebhookService webhookService;
    private final TelegramService telegramService;
    private final ServerInfo serverInfo;
    private final Optional<TelegramVerifyBot> telegramVerifyBot;
    private final ImageService imageService;


    public ApplicationListener(WebhookService webhookService, TelegramService telegramService, ServerInfo serverInfo, Optional<TelegramVerifyBot> telegramVerifyBot, ImageService imageService) {
        this.webhookService = webhookService;
        this.telegramService = telegramService;
        this.serverInfo = serverInfo;
        this.telegramVerifyBot = telegramVerifyBot;
        this.imageService = imageService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        log.debug("runAfterStartup()");
        webhookService.init();
        TelegramBotsLongPollingApplication botsApplication = null;
        if (serverInfo.getUseTelegramBot() || serverInfo.getUseTelegramVerifyBot()) {
            botsApplication = new TelegramBotsLongPollingApplication();
        }

        if (serverInfo.getUseTelegramBot()) {
            String botToken = serverInfo.getTelegramBotToken();
            TelegramBot telegramBot = new TelegramBot(botToken);
            telegramBot.init();
            try {
                botsApplication.registerBot(botToken, telegramBot);
                telegramService.setTelegramBot(telegramBot);
                telegramService.sendMessage("5759660343", "Space-API started at " + startedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " running on " + "ENV: *" + env + "* | VER: *" + VERSION + "*");
            } catch (TelegramApiException ex) {
                log.error("Telegram bot failed to registered!");
            }
        }

        if (serverInfo.getUseTelegramVerifyBot() && telegramVerifyBot.isPresent()) {
            try {
                log.info("Telegram Verify Bot registered successfully.");
                botsApplication.registerBot(serverInfo.getTelegramVerifyBotToken(), telegramVerifyBot.get());
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
}
