package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.model.pcv.*;
import me.xap3y.space.service.ApiKeyService;
import me.xap3y.space.util.ConfigDb;
import me.xap3y.space.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PlaycoreWebInSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public final ConcurrentHashMap<String, PlaycoreStorageModel> storage = new ConcurrentHashMap<>();
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;
    private final PlaycoreWebOutSocketHandler playcoreWebOutSocketHandler;
    private final ServerInfo serverInfo;

    public PlaycoreWebInSocketHandler(ApiKeyService apiKeyService, ObjectMapper objectMapper, PlaycoreWebOutSocketHandler playcoreWebOutSocketHandler, ServerInfo serverInfo) {
        scheduler.scheduleAtFixedRate(this::closeInactiveSessions, 1, 1, TimeUnit.MINUTES);
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
        this.playcoreWebOutSocketHandler = playcoreWebOutSocketHandler;
        this.serverInfo = serverInfo;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connection attempt");
        var uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        log.info("Connection attempt == {}", uri);

        String apiKey = Utils.extractQueryParam(uri.getQuery(), "apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Missing apiKey attempt: {}", apiKey);
            session.close(new CloseStatus(4001, "Missing apiKey"));
            return;
        }

        if (!apiKeyService.validateApiKeySimple(apiKey)) {
            log.info("Invalid apiKey attempt: {}", apiKey);
            session.close(new CloseStatus(4003, "Invalid apiKey"));
            return;
        }

        String version = Utils.extractQueryParam(uri.getQuery(), "version");
        if (version == null || version.isBlank()) {
            log.info("Missing version attempt: {}", apiKey);
            session.close(new CloseStatus(4001, "Missing version"));
            return;
        }

        String uniqueId = Utils.generateRandomId(20).toUpperCase(Locale.ROOT);

        sendJson(session,
                Map.of(
                    "uniqueId", uniqueId,
                    "rawUrl", serverInfo.getBaseUrl() + "/v1/pcv/data/" + uniqueId,
                    "portalUrl", serverInfo.getFrontEndUrl() + "/pcv/" + uniqueId
                )
        );

        storage.put(uniqueId, new PlaycoreStorageModel(version, uniqueId));
        sessions.put(uniqueId, session);
        ConfigDb.availableWsUniqueIds.add(uniqueId);
        lastActivity.put(session.getId(), Instant.now());
    }

    @Override
    public void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        lastActivity.put(session.getId(), Instant.now());
        String payload = message.getPayload();

        log.info("Received message: {}", payload);

        String uniqueId = findKeyBySessionId(session.getId());

        if (payload.equals("CODES_READY") || payload.equals("VIP_READY") || payload.equals("ACTIVE_READY")
                || payload.equals("PAUSED_PACKAGES_READY") || payload.equals("VIP_UPDATE")
                || payload.equals("ACTIVE_UPDATE") || payload.equals("PAUSED_UPDATE") || payload.equals("CODE_UPDATE")) {
            playcoreWebOutSocketHandler.broadcastMessageRaw(uniqueId, payload);
            return;
        }
        // check format
        if (!payload.startsWith("{") || !payload.endsWith("}")) {
            session.sendMessage(new TextMessage("{\"error\":\"Invalid format\", \"received\":\"" + payload + "\"}"));
            return;
        }

        WebSocketMessage wrapper = objectMapper.readValue(payload, WebSocketMessage.class);

        switch (wrapper.getType()) {
            case "VIP" -> {
                playcoreWebOutSocketHandler.broadcastMessageRaw(uniqueId, payload);
                VipPackage vip = objectMapper.treeToValue(wrapper.getData(), VipPackage.class);
                handleVip(session, vip);
            }
            case "CODE" -> {
                playcoreWebOutSocketHandler.broadcastMessageRaw(uniqueId, payload);
                PlaycoreCode code = objectMapper.treeToValue(wrapper.getData(), PlaycoreCode.class);
                handleCode(session, code);
            }
            case "ACTIVE_VIP" -> {
                playcoreWebOutSocketHandler.broadcastMessageRaw(uniqueId, payload);
                ActivePackage vip = objectMapper.treeToValue(wrapper.getData(), ActivePackage.class);
                handleActiveVip(session, vip);
            }
            case "PAUSED_PACKAGE" -> {
                PausedPackage paused = objectMapper.treeToValue(wrapper.getData(), PausedPackage.class);
                String key = findKeyBySessionId(session.getId());
                if (key != null) storage.get(key).addOrReplacePausedPackage(paused);
                playcoreWebOutSocketHandler.broadcastMessageRaw(uniqueId, payload);
            }
            case "DELETE" -> {
                ResourceDelete delete = objectMapper.treeToValue(wrapper.getData(), ResourceDelete.class);
                PlaycoreStorageModel model = storage.get(uniqueId);
                if (model != null) {
                    switch (delete.getType()) {
                        case "CODE" -> model.getCodes().removeIf(c -> c.getCode().equals(delete.getUniqueId()));
                        case "VIP" -> model.getVipPackages().removeIf(v -> v.getName().equals(delete.getUniqueId()));
                        case "ACTIVE_VIP" -> model.getActivePackages().removeIf(a -> a.getPlayerUniqueId().equals(delete.getUniqueId()));
                    }
                }
                playcoreWebOutSocketHandler.broadcastMessageRaw(uniqueId, payload);
            }
            case "ACCEPT", "ERROR" -> {
                playcoreWebOutSocketHandler.broadcastMessageRaw(uniqueId, payload);
            }
            default -> {
                session.sendMessage(new TextMessage("{\"error\":\"Unknown type\", \"received\":\"" + wrapper.getType() + "\"}"));
            }
        }
    }

    public void clearDataForSession(String uniqueId) {
        PlaycoreStorageModel model = storage.get(uniqueId);
        if (model != null) {
            model.getVipPackages().clear();
            model.getActivePackages().clear();
            model.getPausedPackages().clear();
            model.getCodes().clear();
        }
    }

    public void sendMessageToSession(String uniqueId, String message) {
        WebSocketSession session = sessions.get(uniqueId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("Error sending message to session: {}", uniqueId, e);
            }
        }
    }

    @SneakyThrows
    public void postDeleteResource(String uniqueId, ResourceDelete delete) {
        WebSocketSession session = sessions.get(uniqueId);
        PlaycoreStorageModel model = storage.get(uniqueId);
        if (model == null) return;

        switch (delete.getType()) {
            case "CODE" -> model.getCodes().removeIf(c -> c.getCode().equals(delete.getUniqueId()));
            case "VIP" -> model.getVipPackages().removeIf(vip -> vip.getName().equals(delete.getUniqueId()));
            case "ACTIVE_VIP" -> model.getActivePackages().removeIf(activeVip -> activeVip.getPlayerUniqueId().equals(delete.getUniqueId()));
            case "PAUSED_VIP" -> {
                String[] parts = delete.getUniqueId().split(":", 2);
                if (parts.length == 2) model.getPausedPackages().removeIf(p -> p.getUuid().equals(parts[0]) && p.getPackageUi().equals(parts[1]));
            }
        }

        sendJson(session, Map.of(
                "type", "DELETE",
                "data", delete
        ));
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        String uniqueId = findKeyBySessionId(session.getId());
        if (uniqueId == null) return;
        playcoreWebOutSocketHandler.closeAllByUniqueId(uniqueId);
        ConfigDb.availableWsUniqueIds.remove(uniqueId);
        sessions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
        lastActivity.remove(session.getId());
    }

    private void sendJson(WebSocketSession session, Object obj) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(obj)));
    }

    public void sendJsonToSession(String uniqueId, Object obj) throws IOException {
        WebSocketSession session = sessions.get(uniqueId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(obj)));
            log.info("SENT OUT MESSAGE TO SESSION {}: {}", session.getId(), objectMapper.writeValueAsString(obj));
            lastActivity.put(session.getId(), Instant.now());
        }
    }

    private void handleVip(WebSocketSession session, VipPackage vip) {
        String key = findKeyBySessionId(session.getId());
        if (key == null) return;
        storage.get(key).addOrReplaceVipPackage(vip);
    }

    private void handleActiveVip(WebSocketSession session, ActivePackage vip) {
        String key = findKeyBySessionId(session.getId());
        if (key == null) return;
        storage.get(key).addOrReplaceActivePackage(vip);
    }

    private void handleCode(WebSocketSession session, PlaycoreCode code) {
        String key = findKeyBySessionId(session.getId());
        if (key == null) return;
        storage.get(key).addOrReplaceCode(code);
    }

    private void closeInactiveSessions() {
        Instant now = Instant.now();
        for (Map.Entry<String, Instant> entry : lastActivity.entrySet()) {
            if (Duration.between(entry.getValue(), now).toMinutes() >= 30) {
                String id = entry.getKey();
                WebSocketSession session = findSessionById(id);
                if (session != null && session.isOpen()) {
                    try {
                        session.close(CloseStatus.SESSION_NOT_RELIABLE);
                        log.info("Closed inactive session: {}", id);
                    } catch (Exception ignored) {}
                }
                lastActivity.remove(id);
            }
        }
    }

    private WebSocketSession findSessionById(String id) {
        for (WebSocketSession session : sessions.values()) {
            if (session.getId().equals(id)) {
                return session;
            }
        }
        return null;
    }

    private String findKeyBySessionId(String id) {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getValue().getId().equals(id)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean isConnected(String uniqueId) {
        WebSocketSession session = sessions.get(uniqueId);
        return session != null && session.isOpen();
    }
}

// `{"type":"DELETE","data":{"type": "code|vip|active_vip", "uid":"uid"}}`
