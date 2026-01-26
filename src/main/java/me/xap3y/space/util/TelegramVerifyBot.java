package me.xap3y.space.util;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.api.enums.UserAccountStatus;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.entity.*;
import me.xap3y.space.handler.VerifyWebSocketHandler;
import me.xap3y.space.mapper.ImageMapper;
import me.xap3y.space.service.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.CopyTextButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TelegramVerifyBot implements LongPollingSingleThreadUpdateConsumer {

    private final ServerInfo serverInfo;
    private final EmailVerifyCodeService emailVerifyCodeService;
    private final TelegramConnectionService telegramConnectionService;
    private final UserService userService;
    private final VerifyWebSocketHandler verifyWebSocketHandler;
    private final ImageService imageService;
    private final ImageMapper imageMapper;
    private final TelegramConnectCodesService telegramConnectCodesService;
    private final UrlService urlService;
    private final AuditLogService auditLogService;

    public TelegramVerifyBot(ServerInfo serverInfo, EmailVerifyCodeService emailVerifyCodeService, TelegramConnectionService telegramConnectionService, UserService userService, VerifyWebSocketHandler verifyWebSocketHandler, ImageService imageService, ImageMapper imageMapper, TelegramConnectCodesService telegramConnectCodesService, UrlService urlService, AuditLogService auditLogService) {
        this.serverInfo = serverInfo;
        this.emailVerifyCodeService = emailVerifyCodeService;
        this.telegramConnectionService = telegramConnectionService;
        this.userService = userService;
        this.verifyWebSocketHandler = verifyWebSocketHandler;

        init();
        this.imageService = imageService;
        this.imageMapper = imageMapper;
        this.telegramConnectCodesService = telegramConnectCodesService;
        this.urlService = urlService;
        this.auditLogService = auditLogService;
    }

    private TelegramClient telegramClient;

    public void init() {
        telegramClient = new OkHttpTelegramClient(serverInfo.getTelegramVerifyBotToken());
    }

    @Override
    public void consume(Update update) {
        //log.info("Received update: {}", update);
        try {
            if (update.getMessage() != null) {
                //log.info("Got message: {}", update.getMessage());
                handleMessage(update.getMessage());
            } else if (update.getCallbackQuery() != null) {
                // Not used here
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ///////////////////////
    ///    MSG HANDLE   ///
    ///////////////////////

    private void handleMessage(Message msg) throws Exception {
        if (msg.hasPhoto() || msg.hasVideo()) {
            log.info("MEDIA MESSAGE");
            Optional<TelegramConnection> telCon = telegramConnectionService.findByTelegramId(msg.getFrom().getId().toString());
            if (telCon.isEmpty()) {
                send(msg.getChatId(), "You need to verify your account first. Send /start <token> from the website link, or the 6‑digit code if you already have one.");
                return;
            } else if (telCon.get().getUserId().getStatus() != UserAccountStatus.ACTIVE) {
                send(msg.getChatId(), "Your account is not active. Please complete the verification process on the website.");
                return;
            }

            log.info("Processing media message from user: {}", msg.getFrom().getId());

            User user = telCon.get().getUserId();
            String mediaId;
            if (msg.hasPhoto()) {
                var photos = msg.getPhoto();
                mediaId = photos.getLast().getFileId();
            } else if (msg.hasVideo()) {
                mediaId = msg.getVideo().getFileId();
            } else {
                send(msg.getChatId(), "No valid photo or video found in the message.");
                return;
            }

            Image image = downloadImage(mediaId, user);
            ImageInfoDto imgInfo = imageMapper.apply(image);
            send(msg.getChatId(), imgInfo.urlSet().userPreference(), msg.getMessageId());
            return;
        } else {
            //log.info("NO PHOTO MESSAGE");
            // check if message contains attachments
            if (msg.hasDocument()) {
                String name = msg.getDocument().getFileName();
                if (ConfigDb.isImage(name)) {
                    // get URL of the doc
                    Optional<TelegramConnection> telCon = telegramConnectionService.findByTelegramId(msg.getFrom().getId().toString());
                    if (telCon.isEmpty()) {
                        send(msg.getChatId(), "You need to verify your account first. Send /start <token> from the website link, or the 6‑digit code if you already have one.");
                        return;
                    }
                    if (telCon.get().getUserId().getStatus() != UserAccountStatus.ACTIVE) {
                        send(msg.getChatId(), "Your account is not active. Please complete the verification process on the website.");
                        return;
                    }

                    log.info("Processing document message from user: {}", msg.getFrom().getId());
                    User user = telCon.get().getUserId();
                    Image image = downloadImage(msg.getDocument().getFileId(), user);
                    ImageInfoDto imgInfo = imageMapper.apply(image);
                    send(msg.getChatId(), imgInfo.urlSet().portalUrl(), msg.getMessageId());
                    return;
                }
            }
        }

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
        } else if (text.startsWith("/revoke")) {
            revokeTelegramConnection(msg);
            return;
        } else if (text.startsWith("/account")) {
            log.info("ACCOUNT INFO COMMAND");
            accountInfoCommand(msg);
            return;
        } else if (text.startsWith("https://")) {
            log.info("SHORT URL COMMAND");
            shortUrlCommand(msg);
            return;
        } else if (text.startsWith("/help")) {
            helpCommand(msg);
            return;
        }

        Optional<TelegramConnection> telCon = telegramConnectionService.findByTelegramId(msg.getFrom().getId().toString());
        if (telCon.isEmpty()) {
            send(msg.getChatId(), "Send /start <token> from the website link, or the 6‑digit code if you already have one.");
        } else {
            send(msg.getChatId(), "Unknown command. Run **/help** for a list of commands.");
        }
    }

    private void helpCommand(Message msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Available Commands:**\n");
        sb.append("/account - View your account information.\n");
        sb.append("/revoke - Revoke your current Telegram connection.\n");
        sb.append("Send any image or video to upload it.\n");
        sb.append("Send a valid URL (starting with https://) to shorten it.\n");
        send(msg.getChatId(), sb.toString());
    }

    private void shortUrlCommand(Message msg) {
        Optional<TelegramConnection> telCon = telegramConnectionService.findByTelegramId(msg.getFrom().getId().toString());
        if (telCon.isEmpty()) {
            send(msg.getChatId(), "You are not connected to any Telegram account.");
            return;
        }

        if (msg.getText().contains(" ")) {
            send(msg.getChatId(), "Please send a valid URL without spaces.");
            return;
        }

        String url = msg.getText().trim(); // Assuming the entire message is the URL

        ShortUrlDto urlDto = urlService.createUrl(url, telCon.get().getUserId(), -1, null);

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder()
                .text("Copy Short URL")
                .copyText(CopyTextButton.builder().text((urlDto.urlSet().userPreference() == null) ? urlDto.urlSet().rawUrl() : urlDto.urlSet().userPreference()).build())
                .build());
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();

        send(msg.getChatId(), urlDto.urlSet().rawUrl(), msg.getMessageId(), kb);
    }

    private void revokeTelegramConnection(Message msg) {
        Optional<TelegramConnection> telCon = telegramConnectionService.findByTelegramId(msg.getFrom().getId().toString());
        if (telCon.isEmpty()) {
            send(msg.getChatId(), "You are not connected to any Telegram account.");
            return;
        }

        telegramConnectionService.revokeByUserId(telCon.get().getUserId().getId());
        auditLogService.saveLog(PortalLogType.TELEGRAM_REVOKED, telCon.get().getUserId(), telCon.get().getId().toString(), "TG");
        send(msg.getChatId(), "Your Telegram connection has been revoked.");
    }


    private void accountInfoCommand(Message msg) {
        Optional<TelegramConnection> telCon = telegramConnectionService.findByTelegramId(msg.getFrom().getId().toString());
        if (telCon.isEmpty()) {
            send(msg.getChatId(), "You are not connected to any Telegram account.");
            return;
        }

        User user = telCon.get().getUserId();
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDC64 Account Information:\n");
        sb.append("\uD83C\uDFF7️ Username: ").append(user.getUsername()).append(" (").append(user.getId()).append(")\n");
        sb.append("✉️ Email: ").append(user.getEmail()).append("\n");

        String statusEmoji = switch (user.getStatus()) {
            case ACTIVE -> "\uD83D\uDFE2"; // White Heavy Check Mark
            case WAITING_VERIFICATION -> "\u23F3"; // Hourglass Not Done
            case SUSPENDED -> "\u274C"; // Cross Mark
            case INACTIVE -> "\u26D4"; // No Entry
            default -> "";
        };

        auditLogService.saveLog(PortalLogType.TELEGRAM_BOT_COMMAND, user, "account_info", null);

        sb.append(statusEmoji).append(" Status: ").append(user.getStatus().name()).append("\n");
        sb.append("\uD83D\uDCC5 Created at: ").append(user.getCreatedAt()).append("\n");

        send(msg.getChatId(), sb.toString());
    }

    private void processConnectCommand(Message msg, String token) {
        if (telegramConnectionService.existsByTelegramId(msg.getFrom().getId().toString())) {
            send(msg.getChatId(), "This Telegram account is already connected.");
            return;
        }

        Optional<TelegramConnectCodes> code = telegramConnectCodesService.findByCodeNotUsed(token);
        if (code.isEmpty()) {
            send(msg.getChatId(), "Connection code not found.");
            return;
        }
        else if (code.get().getUser().getStatus() != UserAccountStatus.ACTIVE) {
            send(msg.getChatId(), "Your user account is not active. Please verify your email first.");
            return;
        }

        String userId = msg.getFrom().getId().toString();
        String username = msg.getFrom().getUserName();
        GetUserProfilePhotos getPhotos = new GetUserProfilePhotos(msg.getFrom().getId());
        getPhotos.setLimit(1);
        String avatarUrl = null;

        try {
            UserProfilePhotos photos = telegramClient.execute(getPhotos);
            if (photos.getTotalCount() > 0) {
                PhotoSize photo =
                        photos.getPhotos().getFirst()
                                .getLast();

                File file = telegramClient.execute(
                        new GetFile(photo.getFileId())
                );

                String url = "https://api.telegram.org/file/bot" + serverInfo.getTelegramVerifyBotToken() + "/" + file.getFilePath();
                String name = file.getFilePath().replaceAll("/", "_");
                boolean saved = imageService.saveToTempFileFromUrl(url,name);
                if (saved)
                    avatarUrl = serverInfo.getBaseUrl() + "/v1/image/get/temp/" + name;
            }
        } catch (TelegramApiException e) {
            log.warn("Could not get profile photos for user {}: {}", msg.getFrom().getId(), e.getMessage());
        }

        TelegramConnection connection = new TelegramConnection(
                code.get().getUser(),
                userId,
                username,
                avatarUrl,
                msg.getFrom().getFirstName() + (msg.getFrom().getLastName() != null ? " " + msg.getFrom().getLastName() : "")
        );

        telegramConnectionService.save(connection);
        verifyWebSocketHandler.pushVerified(token);
        telegramConnectCodesService.setCodeUsed(code.get());
        auditLogService.saveLog(PortalLogType.TELEGRAM_CONNECTED, code.get().getUser(), connection.getId().toString(), null);
        InlineKeyboardMarkup kb = null;

        if (code.get().getFallback() != null && !code.get().getFallback().isBlank() && code.get().getFallback().startsWith("https://")) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(InlineKeyboardButton.builder()
                    .text("Return to Website")
                    .url(code.get().getFallback())
                    .build());
            kb = InlineKeyboardMarkup.builder()
                    .keyboardRow(row)
                    .build();
        }

        send(msg.getChatId(), "You have successfully connected your Telegram account. You can now send images to this bot to upload them and urls.", null, kb);
    }

    private void processStartCommand(Message msg, String token) throws Exception {
        if (token == null || token.isBlank()) {
            send(msg.getChatId(), "Missing token. Please return to the website and click the Telegram verification button again.");
            return;
        }

        if (token.length() == 55) {
            processConnectCommand(msg, token);
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

    private static String escapeMdV2Text(String s) {
        return s.replaceAll("(?<!\\\\)([_\\[\\]\\(\\)~`>#+\\-=|{}.!])", "\\\\$1");
    }

    public void send(Long chatId, String message, Integer messageId){
        send(chatId, message, messageId, null);
    }

    public void send(Long chatId, String message, Integer messageId, InlineKeyboardMarkup kb) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(escapeMdV2Text(message))
                .parseMode(ParseMode.MARKDOWNV2)
                .replyToMessageId(messageId)
                .disableWebPagePreview(true)
                .build();
        if (kb != null) {
            sendMessage.setReplyMarkup(kb);
        }
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }

    public void send(Long chatId, String message) {
        send(chatId, message, null);
    }

    private Image downloadImage(String fileId, User user) throws Exception {
        var filePath = telegramClient.execute(new GetFile(fileId)).getFilePath();

        String fileUrl = "https://api.telegram.org/file/bot" + serverInfo.getTelegramVerifyBotToken() + "/" + filePath;
        log.info("Telegram Download URL: {}", fileUrl);

        return imageService.saveImageFromUrl(fileUrl, user, ResourceSourceType.TELEGRAM);
    }

}
