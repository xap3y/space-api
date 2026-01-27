package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.iface.OptionalCookieAuth;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.entity.TelegramConnectCodes;
import me.xap3y.space.entity.TelegramConnection;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ShortUserMapper;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.AuditLogService;
import me.xap3y.space.service.EmailVerifyCodeService;
import me.xap3y.space.service.TelegramConnectCodesService;
import me.xap3y.space.service.TelegramConnectionService;
import me.xap3y.space.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/telegram")
@AllArgsConstructor
public class TelegramController {

    private final TelegramConnectionService telegramConnectionService;
    private final ShortUserMapper shortUserMapper;
    private final TelegramConnectCodesService telegramConnectCodesService;
    private final AuditLogService auditLogService;
    private final ServerInfo serverInfo;

    @GetMapping(
            value = "/@me",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> getTelegramConnection(
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");

        TelegramConnection connection = telegramConnectionService.findByUserStrict(uploader.getId());
        ShortUserDto userDto = shortUserMapper.apply(connection.getUserId());

        Map<String, Object> map = new HashMap<>() {{
            put("telegram_id", connection.getTelegramId());
            put("user", userDto);
            put("connected_at", connection.getConnectedAt());
            put("full_name", connection.getFullName());
            put("username", connection.getUsername());
            put("avatar", connection.getAvatar());
        }};

        return new ResponseEntity<>(new DefaultResponse(false, map), HttpStatus.OK);
    }

    @GetMapping(
            value = "/getcode",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> connectToTelegramUser(
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");

        if (telegramConnectionService.existsByUserId(uploader.getId())) {
            return new ResponseEntity<>(new DefaultResponse(true, "User already connected to Telegram"), HttpStatus.BAD_REQUEST);
        } else if (!serverInfo.getUseTelegramVerifyBot()) {
            return new ResponseEntity<>(new DefaultResponse(false, "Service unavailable"), HttpStatus.SERVICE_UNAVAILABLE);
        }

        TelegramConnectCodes codeToUse = telegramConnectCodesService.findByUserIdNotUsed(uploader.getId())
                .orElseGet(() -> {
                    TelegramConnectCodes newCode = new TelegramConnectCodes();
                    newCode.setUser(uploader);
                    newCode.setExpiresAt(LocalDateTime.now().plusMinutes(30));
                    newCode.setCreatedAt(LocalDateTime.now());
                    String code;
                    do {
                        code = Utils.generateRandomId(55);
                    } while (telegramConnectCodesService.findByCode(code).isPresent());
                    newCode.setCode(code);
                    return telegramConnectCodesService.save(newCode);
                });

        Map<String, Object> map = Map.of(
                "code", codeToUse.getCode()
        );
        return new ResponseEntity<>(new DefaultResponse(false, map), HttpStatus.OK);
    }

    @GetMapping(
            value = "/@me/revoke",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    @OptionalCookieAuth
    public ResponseEntity<?> revokeTelegramConnection(
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");
        TelegramConnection connection = telegramConnectionService.findByUserStrict(uploader.getId());

        auditLogService.saveLog(PortalLogType.TELEGRAM_REVOKED, connection.getUserId(), connection.getId().toString(), "API");
        telegramConnectionService.revokeByUserId(connection.getUserId().getId());
        return new ResponseEntity<>(new DefaultResponse(false, "Revoked"), HttpStatus.NO_CONTENT);
    }

    @GetMapping(
            value = "/connect/request",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresApiKey
    @OptionalCookieAuth
    public ResponseEntity<?> requestConnectToTelegram(
            HttpServletRequest request,
            @RequestParam(value = "fallback", required = false) String fallbackUrl
    ) {
        User uploader = (User) request.getAttribute("uploader");

        boolean exists = telegramConnectionService.existsByUserId(uploader.getId());
        if (exists) {
            throw new BadRequestException("User already connected to Telegram");
        }
        else if (!serverInfo.getUseTelegramVerifyBot()) {
            return new ResponseEntity<>(new DefaultResponse(false, "Service unavailable"), HttpStatus.SERVICE_UNAVAILABLE);
        }

        TelegramConnectCodes newCode = new TelegramConnectCodes();
        newCode.setUser(uploader);
        newCode.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        newCode.setCreatedAt(LocalDateTime.now());
        newCode.setCode(Utils.generateRandomId(55));
        newCode.setFallback(fallbackUrl);
        telegramConnectCodesService.save(newCode);

        Map<String, String> res = Map.of(
                "botname", serverInfo.getTelegramVerifyBotName(),
                "url", "https://t.me/" + serverInfo.getTelegramVerifyBotName() + "?start=" + newCode.getCode(),
                "token", newCode.getCode()
        );

        return new ResponseEntity<>(new DefaultResponse(false, res), HttpStatus.OK);
    }
}
