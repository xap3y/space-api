package me.xap3y.space.service;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.util.TelegramVerifyBot;
import org.springframework.stereotype.Service;

@Service
public class TelegramVerifyService {

    private final ServerInfo serverInfo;
    private final TelegramVerifyBot telegramBot;

    public TelegramVerifyService(ServerInfo serverInfo, TelegramVerifyBot telegramBot) {
        this.serverInfo = serverInfo;
        this.telegramBot = telegramBot;
    }

    public void sendMessage(Long chatId, String message) {
        if (!serverInfo.getUseTelegramBot()) return;
        telegramBot.send(chatId, message);
    }

    public void sendVerifyCode(Long chatId, String code) {
        if (!serverInfo.getUseTelegramBot()) return;
        String message = "Your verification code is: " + code;
        telegramBot.send(chatId, message);
    }
}
