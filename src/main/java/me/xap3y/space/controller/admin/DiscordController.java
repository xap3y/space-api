package me.xap3y.space.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.iface.OptionalCookieAuth;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.discord.Emoji;
import me.xap3y.space.dto.DiscordMeDto;
import me.xap3y.space.entity.DiscordConnection;
import me.xap3y.space.entity.User;
import me.xap3y.space.model.request.DiscordConnectionRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.DiscordConnectionService;
import me.xap3y.space.service.RemoteMessageService;
import me.xap3y.space.service.SessionService;
import me.xap3y.space.service.TwoFactorService;
import me.xap3y.space.util.Utils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/v1/discord")
@AllArgsConstructor
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class DiscordController {

    private final SessionService sessionService;
    private final RemoteMessageService remoteMessageService;
    private final ServerInfo serverInfo;
    private final Utils utils;
    private DiscordConnectionService discordConnectionService;
    private final TwoFactorService twoFactorService;

    @GetMapping(
            value = "/get/@me",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> getDiscordConnection(
            HttpServletRequest request
    ) {

        User uploader = (User) request.getAttribute("uploader");

        DiscordConnection connection = discordConnectionService.findByUserId(uploader).orElseThrow(() -> new BadRequestException("You don't have a Discord connection"));

        if (connection.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Your Discord connection has expired");
        }

        return new ResponseEntity<>(new DefaultResponse(false, connection), HttpStatus.OK);
    }

    @GetMapping(
            value = "/get/{discordId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getDiscordConnection(
            @PathVariable(name = "discordId") String discordId,
            HttpServletRequest request
    ) {
        DiscordConnection connection = discordConnectionService.findByDiscordId(discordId).orElseThrow(() -> new BadRequestException("This ID doesn't have a Discord connection"));

        if (connection.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Your Discord connection has expired");
        }

        return new ResponseEntity<>(new DefaultResponse(false, connection), HttpStatus.OK);
    }

    @DeleteMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    @OptionalCookieAuth
    public ResponseEntity<?> deauthorizeDiscordConnection(
            @PathVariable(name = "uniqueId") String uniqueId,
            HttpServletRequest request
    ) {
        DiscordConnection connection = discordConnectionService.findByDiscordId(uniqueId).orElseThrow(() -> new BadRequestException("You don't have a Discord connection"));

        boolean revokedOnDiscordApi = discordConnectionService.revokeDiscordConnection(connection.getAccessToken());
        discordConnectionService.deleteByDiscordId(uniqueId);

        remoteMessageService
                .sendDiscordBotMessage(Emoji.WARNING.getUnicode() + " Discord connection for user: " + Utils.structDiscordUserTag(connection.getDiscordId()) + " was deauthorized by " + utils.structDiscordProfileLink(connection.getUserId().getUsername()))
                .subscribe();

        return new ResponseEntity<>(new DefaultResponse(false, "Successfully deauthorized Discord connection for userId: " + uniqueId + ", discord api res: " + revokedOnDiscordApi), HttpStatus.OK);
    }

    @DeleteMapping(
            value = "/token/{token}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> revokeAuthToken(
            @PathVariable(name = "token") String token,
            HttpServletRequest request
    ) {
        //DiscordConnection connection = discordConnectionService.findByAccessToken(token).orElseThrow(() -> new BadRequestException("You don't have a Discord connection"));

        boolean revokedOnDiscordApi = discordConnectionService.revokeDiscordConnection(token);
        //discordConnectionService.deleteByAccessToken(token);

        return new ResponseEntity<>(new DefaultResponse(false, "Successfully revoked Discord token connection for userId: " + null + ", discord api res: " + revokedOnDiscordApi), HttpStatus.OK);
    }

    @DeleteMapping(
            value = "/get/@me",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> deauthorizeDiscordConnection(
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");

        DiscordConnection connection = discordConnectionService.findByUserId(uploader).orElseThrow(() -> new BadRequestException("You don't have a Discord connection"));

        boolean revokedOnDiscordApi = discordConnectionService.revokeDiscordConnection(connection.getAccessToken());
        discordConnectionService.deleteByDiscordId(connection.getDiscordId());

        remoteMessageService
                .sendDiscordBotMessage(Emoji.WARNING.getUnicode() + " Discord connection for user: " + utils.structDiscordProfileLink(connection.getUserId().getUsername()) + " (" + Utils.structDiscordUserTag(connection.getDiscordId()) +") was deauthorized by " + utils.structDiscordProfileLink(uploader.getUsername()))
                .subscribe();

        return new ResponseEntity<>(new DefaultResponse(false, "Successfully deauthorized Discord connection, discord api: " + revokedOnDiscordApi), HttpStatus.OK);
    }

    @PostMapping(
            value = "/authorize",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> authorizeDiscordConnection(
            HttpServletRequest request,
            @RequestBody DiscordConnectionRequest body
    ) throws JsonProcessingException {
        User uploader = (User) request.getAttribute("uploader");

        if (body.getAccessToken() == null) {
            throw new BadRequestException("Missing access token");
        }

        if (discordConnectionService.existsByUserId(uploader)) {
            throw new BadRequestException("You already have a Discord connection!");
        }

        DiscordMeDto connectionDto = discordConnectionService.fetchDiscordMe(body.getAccessToken());

        discordConnectionService.findByDiscordId(connectionDto.id()).ifPresent(
                existingConnection -> {
                    throw new BadRequestException("You already have a Discord connection with this ID: " + connectionDto.id());
                }
        );

        DiscordConnection discordConnection = new DiscordConnection();
        discordConnection.setUserId(uploader);
        discordConnection.setDiscordId(connectionDto.id());
        discordConnection.setEmail(connectionDto.email());
        discordConnection.setGlobalName(connectionDto.globalName());
        discordConnection.setUsername(connectionDto.username());
        discordConnection.setConnectedAt(LocalDateTime.now());
        discordConnection.setAvatar(connectionDto.avatar());

        discordConnection.setAccessToken(body.getAccessToken());
        discordConnection.setRefreshToken(body.getRefreshToken());
        discordConnection.setExpiresAt(Instant.now().plusSeconds(body.getExpiresAt()));

        discordConnectionService.save(discordConnection);

        remoteMessageService
                .sendDiscordBotMessage(Emoji.INFO.getUnicode() + " New Discord connection for user: " + utils.structDiscordProfileLink(uploader.getUsername()) + " (<@" + connectionDto.id() + ">)")
                .subscribe();

        return new ResponseEntity<>(new DefaultResponse(false, discordConnection), HttpStatus.OK);
    }

    @PostMapping(
            value = "/login",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> authorizeDiscordConnectionLogin(
            HttpServletRequest request,
            @RequestBody DiscordConnectionRequest body
    ) throws JsonProcessingException {
        if (body.getAccessToken() == null) {
            throw new BadRequestException("Missing access token");
        }

        //DiscordConnection connection = discordConnectionService.findByAccessToken(body.getAccessToken()).orElseThrow(() -> new BadRequestException("You don't have a Discord connection"));

        DiscordMeDto connectionDto = discordConnectionService.fetchDiscordMe(body.getAccessToken());

        Optional<DiscordConnection> connection = discordConnectionService.findByDiscordId(connectionDto.id());

        if (connection.isEmpty()) {
            discordConnectionService.revokeDiscordConnection(body.getAccessToken());
            throw new BadRequestException("You don't have a Discord connection");
        }

        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        User loginUser = connection.get().getUserId();
        if (twoFactorService.isEnabled(loginUser)) {
            return ResponseEntity.ok(new DefaultResponse(false, java.util.Map.of(
                    "requiresTwoFactor", true,
                    "challengeToken", twoFactorService.createLoginChallenge(loginUser),
                    "expiresInSeconds", 300
            )));
        }

        String sessionToken = sessionService.createSession(loginUser, userAgent, ipAddress);

        ResponseCookie cookie = ResponseCookie.from("session_token", sessionToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new DefaultResponse(false, connectionDto));

    }
}
