package me.xap3y.space.service;

import lombok.Setter;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.util.TelegramBot;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    private final ServerInfo serverInfo;

    @Setter
    private TelegramBot telegramBot;

    public TelegramService(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }


    public void sendMessage(String chatId, String message) {
        if (!serverInfo.getUseTelegramBot()) return;
        telegramBot.sendMessage(chatId, message);
    }

    public void sendImageUrl(String chatId, ImageInfoDto image) {
        if (!serverInfo.getUseTelegramBot()) return;
        String message = "Image uploaded: " + image.uniqueId() + " | Uploaded BY: " + image.uploader().username();
        telegramBot.sendMessage(chatId, message);
    }
}
