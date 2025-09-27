package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.entity.TelegramConnectCodes;
import me.xap3y.space.entity.TelegramConnection;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ShortUserMapper;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.EmailVerifyCodeService;
import me.xap3y.space.service.TelegramConnectCodesService;
import me.xap3y.space.service.TelegramConnectionService;
import me.xap3y.space.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final EmailVerifyCodeService emailVerifyCodeService;
    private final TelegramConnectCodesService telegramConnectCodesService;

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
    public ResponseEntity<?> revokeTelegramConnection(
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");
        TelegramConnection connection = telegramConnectionService.findByUserStrict(uploader.getId());

        telegramConnectionService.revokeByUserId(connection.getUserId().getId());
        return new ResponseEntity<>(new DefaultResponse(false, "Revoked"), HttpStatus.NO_CONTENT);
    }
}
