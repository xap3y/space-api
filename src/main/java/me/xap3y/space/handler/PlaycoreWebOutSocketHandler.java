package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.model.pcv.ActivePackage;
import me.xap3y.space.model.pcv.PlaycoreCode;
import me.xap3y.space.model.pcv.VipPackage;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PlaycoreWebOutSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper objectMapper;

    public PlaycoreWebOutSocketHandler(ObjectMapper objectMapper) {
        scheduler.scheduleAtFixedRate(this::closeInactiveSessions, 1, 1, TimeUnit.MINUTES);
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String uniqueId = Utils.extractQueryParam(uri.getQuery(), "uniqueId");
        if (uniqueId == null || uniqueId.isBlank()) {
            session.close(new CloseStatus(4001, "Missing uniqueId"));
            return;
        }

        if (!ConfigDb.availableWsUniqueIds.contains(uniqueId)) {
            session.close(new CloseStatus(4003, "Invalid uniqueId"));
            return;
        }

        sessions.put(uniqueId, session);
        lastActivity.put(session.getId(), Instant.now());
        sendJson(session, Map.of("type", "channel_ready"));
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        sessions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
        lastActivity.remove(session.getId());
    }

    @SneakyThrows
    public void sendOutVipPackage(String uid, VipPackage vipPackage) {
        String json = objectMapper.writeValueAsString(vipPackage);
        sendJson(sessions.get(uid), Map.of(
                "type", "new_vip",
                "data", json
        ));
    }

    @SneakyThrows
    public void sendOutCode(String uid, PlaycoreCode code) {
        String json = objectMapper.writeValueAsString(code);
        sendJson(sessions.get(uid), Map.of(
                "type", "new_code",
                "data", json
        ));
    }

    @SneakyThrows
    public void sendOutActiveVip(String uid, ActivePackage vip) {
        String json = objectMapper.writeValueAsString(vip);
        sendJson(sessions.get(uid), Map.of(
                "type", "active_vip",
                "data", json
        ));
    }

    private void sendJson(WebSocketSession session, Object obj) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(obj)));
        log.info("SENT OUT MESSAGE TO SESSION {}: {}", session.getId(), objectMapper.writeValueAsString(obj));
        lastActivity.put(session.getId(), Instant.now());

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
                        System.out.println("Closed inactive session: " + id);
                    } catch (Exception ignored) {}
                }
                lastActivity.remove(id);
            }
        }
    }

    public void broadcastMessage(Object obj) {
        sessions.values().forEach(session -> {
            try {
                sendJson(session, obj);
            } catch (IOException e) {
                log.error("Failed to send broadcast message to session {}: {}", session.getId(), e.getMessage());
            }
        });
    }

    public void closeAllByUniqueId(String uniqueId) {
        WebSocketSession session = findSessionByKey(uniqueId);
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                log.error("Failed to close session {}: {}", uniqueId, e.getMessage());
            }
        }
    }

    public void broadcastMessageRaw(String uniqueId, String obj) {
        try {
            WebSocketSession ss = findSessionByKey(uniqueId);
            if (ss != null && ss.isOpen())
                ss.sendMessage(new TextMessage(obj));
        } catch (IOException e) {
            log.error("Failed to send broadcast message to session {}: {}", uniqueId, e.getMessage());
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

    private WebSocketSession findSessionByKey(String key) {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
