package me.xap3y.space.util;

import me.xap3y.space.api.enums.UserAccountStatus;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.entity.EmailVerifyCodes;
import me.xap3y.space.entity.TelegramConnection;
import me.xap3y.space.entity.User;
import me.xap3y.space.handler.VerifyWebSocketHandler;
import me.xap3y.space.service.EmailVerifyCodeService;
import me.xap3y.space.service.TelegramConnectionService;
import me.xap3y.space.service.UserService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class TelegramVerifyBot implements LongPollingSingleThreadUpdateConsumer {

    private final ServerInfo serverInfo;
    private final EmailVerifyCodeService emailVerifyCodeService;
    private final TelegramConnectionService telegramConnectionService;
    private final UserService userService;
    private final VerifyWebSocketHandler verifyWebSocketHandler;

    public TelegramVerifyBot(ServerInfo serverInfo, EmailVerifyCodeService emailVerifyCodeService, TelegramConnectionService telegramConnectionService, UserService userService, VerifyWebSocketHandler verifyWebSocketHandler) {
        this.serverInfo = serverInfo;
        this.emailVerifyCodeService = emailVerifyCodeService;
        this.telegramConnectionService = telegramConnectionService;
        this.userService = userService;
        this.verifyWebSocketHandler = verifyWebSocketHandler;

        init();
    }

    private TelegramClient telegramClient;

    public void init() {
        telegramClient = new OkHttpTelegramClient(serverInfo.getTelegramVerifyBotToken());
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.getMessage() != null) {
                handleMessage(update.getMessage());
            } else if (update.getCallbackQuery() != null) {
                // Not used here
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Message msg) throws Exception {
        String text = msg.getText();
        if (text == null) return;

        text = text.trim();

        if (text.startsWith("/start")) {
            // Format: "/start" or "/start token"
            String token = null;
            String[] parts = text.split("\\s+", 2);
            if (parts.length == 2) {
                token = parts[1];
            }
            processStartCommand(msg, token);
            return;
        }

        send(msg.getChatId(), "Send /start <token> from the website link, or the 6‑digit code if you already have one.");
    }

    private void processStartCommand(Message msg, String token) throws Exception {
        if (token == null || token.isBlank()) {
            send(msg.getChatId(), "Missing token. Please return to the website and click the Telegram verification button again.");
            return;
        }

        boolean exists = emailVerifyCodeService.existsByTelCode(token);

        if (!exists) {
            send(msg.getChatId(), "Verification code not found. Please return to the website and request a new code.");
            return;
        }

        EmailVerifyCodes code = emailVerifyCodeService.findByTelCodeStrict(token);

        if (code.isUsed()) {
            send(msg.getChatId(), "Verification code is already used. Please return to the website and request a new code.");
            return;
        }

        String userId = msg.getFrom().getId().toString();

        telegramConnectionService.findByTelegramId(userId).ifPresent(connection -> {
            if (connection.getUserId().getStatus() == UserAccountStatus.ACTIVE)
                send(msg.getChatId(), "You are already connected to this Telegram account.");
        });

        telegramConnectionService.save(new TelegramConnection(code.getUser(),userId, code));

        //send(msg.getChatId(), "Your verification code is **" + code.getCode() + "**");

        emailVerifyCodeService.setCodeUsed(code);
        userService.updateUserStatus(code.getUser(), UserAccountStatus.ACTIVE);

        verifyWebSocketHandler.pushVerified(code.getTelCode());

        send(msg.getChatId(), "You have successfully verified your account. You can now log in to the website.");
    }

    public void send(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(message.replaceAll("(?<!\\\\)([_\\[\\]\\(\\)~`>#+\\-=|{}.!])", "\\\\$1"))
                .parseMode(ParseMode.MARKDOWNV2)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            // Handle exception
        }
    }

}
