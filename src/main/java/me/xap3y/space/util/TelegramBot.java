package me.xap3y.space.util;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private final String botToken;

    public TelegramBot(String botToken) {
        this.botToken = botToken;
    }

    private TelegramClient telegramClient;

    public void init() {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) { /* IGNORE */ }

    public void sendMessage(String chatId, String message) {
        //SendMessage sendMessage = new SendMessage(chatId, message);

        SendMessage sendMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text(message.replaceAll("(?<!\\\\)([_\\[\\]\\(\\)~`>#+\\-=|{}.!])", "\\\\$1"))
                        .parseMode(ParseMode.MARKDOWNV2)
                        .build();
        //sendMessage.setParseMode(ParseMode.MARKDOWNV2);
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            // Handle exception
        }
    }
}
