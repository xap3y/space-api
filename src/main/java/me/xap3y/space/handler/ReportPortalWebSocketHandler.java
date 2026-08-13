package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.entity.MinecraftServerReports;
import me.xap3y.space.service.MinecraftServerReportsService;
import me.xap3y.space.service.TrSessionService;
import me.xap3y.space.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ReportPortalWebSocketHandler extends TextWebSocketHandler {

    private static final Set<String> CLIENT_ACTIONS = Set.of(
            "list_reports",
            "get_report",
            "comment",
            "close_report",
            "delete_report",
            "get_config",
            "save_config",
            "reload_config"
    );

    private final ObjectMapper objectMapper;
    private final MinecraftServerReportsService reportsService;
    private final TrSessionService trSessionService;
    private final Map<Long, WebSocketSession> pluginSessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> clientSessions = new ConcurrentHashMap<>();
    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> pendingRequests = new ConcurrentHashMap<>();

    public ReportPortalWebSocketHandler(
            ObjectMapper objectMapper,
            MinecraftServerReportsService reportsService,
            TrSessionService trSessionService
    ) {
        this.objectMapper = objectMapper;
        this.reportsService = reportsService;
        this.trSessionService = trSessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getUri() == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String role = Utils.extractQueryParam(session.getUri().getQuery(), "role");
        if (role == null) {
            session.close(new CloseStatus(4001, "Missing credentials"));
            return;
        }

        boolean plugin = "plugin".equalsIgnoreCase(role);
        boolean client = "client".equalsIgnoreCase(role);
        if (!plugin && !client) {
            session.close(new CloseStatus(4002, "Invalid role"));
            return;
        }

        MinecraftServerReports server;
        try {
            if (plugin) {
                String apiKey = Utils.extractQueryParam(session.getUri().getQuery(), "apiKey");
                if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("Missing API key");
                server = reportsService.getServerByApiKeyStrict(apiKey);
            } else {
                String token = extractCookie(session, "tr_token");
                if (token == null || token.isBlank()) throw new IllegalArgumentException("Missing login session");
                server = trSessionService.getValidSession(token).getUser();
            }
        } catch (Exception ex) {
            session.close(new CloseStatus(4003, "Invalid or expired credentials"));
            return;
        }

        if (server.isPaused()) {
            session.close(new CloseStatus(4003, "Server account is paused"));
            return;
        }

        connections.put(session.getId(), new ConnectionInfo(server.getId(), plugin));
        if (plugin) {
            WebSocketSession previous = pluginSessions.put(server.getId(), session);
            if (previous != null && previous.isOpen() && !previous.getId().equals(session.getId())) {
                previous.close(new CloseStatus(4000, "Replaced by a newer plugin connection"));
            }
            broadcastStatus(server.getId(), true);
            log.info("Report portal plugin connected for server {}", server.getServerName());
        } else {
            clientSessions.computeIfAbsent(server.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(session);
            send(session, Map.of(
                    "type", "connection_status",
                    "pluginOnline", isPluginOnline(server.getId()),
                    "serverName", server.getServerName(),
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ConnectionInfo connection = connections.get(session.getId());
        if (connection == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            sendError(session, null, "Invalid JSON message");
            return;
        }

        if ("ping".equals(payload.path("type").asText())) {
            send(session, Map.of("type", "pong", "timestamp", Instant.now().toString()));
            return;
        }

        if (connection.plugin()) {
            handlePluginMessage(connection.serverId(), payload);
        } else {
            handleClientMessage(session, connection.serverId(), payload);
        }
    }

    private void handleClientMessage(WebSocketSession client, Long serverId, JsonNode payload) throws IOException {
        if (!"command".equals(payload.path("type").asText())) {
            sendError(client, payload.path("requestId").asText(null), "Only commands are accepted");
            return;
        }

        String action = payload.path("action").asText();
        if (!CLIENT_ACTIONS.contains(action)) {
            sendError(client, payload.path("requestId").asText(null), "Unsupported command");
            return;
        }

        WebSocketSession plugin = pluginSessions.get(serverId);
        if (plugin == null || !plugin.isOpen()) {
            sendError(client, payload.path("requestId").asText(null), "Report server is offline");
            return;
        }

        String requestId = payload.path("requestId").asText();
        if (requestId.isBlank()) requestId = UUID.randomUUID().toString();

        ObjectNode forwarded = payload.deepCopy();
        forwarded.put("type", "command");
        forwarded.put("requestId", requestId);
        String pendingKey = pendingKey(serverId, requestId);
        if (pendingRequests.putIfAbsent(pendingKey, client) != null) {
            sendError(client, requestId, "Duplicate request ID");
            return;
        }
        send(plugin, forwarded);
    }

    private void handlePluginMessage(Long serverId, JsonNode payload) throws IOException {
        String requestId = payload.path("requestId").asText(null);
        if (requestId != null) {
            WebSocketSession client = pendingRequests.remove(pendingKey(serverId, requestId));
            if (client != null && client.isOpen()) {
                send(client, payload);
                return;
            }
        }
        broadcast(serverId, payload);
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        ConnectionInfo info = connections.remove(session.getId());
        if (info == null) return;

        if (info.plugin()) {
            if (pluginSessions.remove(info.serverId(), session)) {
                failPendingRequests(info.serverId(), "Report server went offline");
                broadcastStatus(info.serverId(), false);
            }
        } else {
            Set<WebSocketSession> clients = clientSessions.get(info.serverId());
            if (clients != null) {
                clients.remove(session);
                if (clients.isEmpty()) clientSessions.remove(info.serverId());
            }
            pendingRequests.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Report portal transport error for {}: {}", session.getId(), exception.getMessage());
        try {
            if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {
        }
    }

    private void broadcastStatus(Long serverId, boolean online) {
        broadcast(serverId, Map.of(
                "type", "connection_status",
                "pluginOnline", online,
                "timestamp", Instant.now().toString()
        ));
    }

    private boolean isPluginOnline(Long serverId) {
        WebSocketSession session = pluginSessions.get(serverId);
        return session != null && session.isOpen();
    }

    private void broadcast(Long serverId, Object payload) {
        Set<WebSocketSession> clients = clientSessions.get(serverId);
        if (clients == null) return;
        clients.removeIf(session -> !session.isOpen());
        clients.forEach(session -> {
            try {
                send(session, payload);
            } catch (IOException ex) {
                log.debug("Failed to send report portal event: {}", ex.getMessage());
            }
        });
    }

    private void sendError(WebSocketSession session, String requestId, String message) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "response");
        if (requestId != null) response.put("requestId", requestId);
        response.put("success", false);
        response.put("error", message);
        send(session, response);
    }

    private void send(WebSocketSession session, Object payload) throws IOException {
        String json = payload instanceof JsonNode node
                ? objectMapper.writeValueAsString(node)
                : objectMapper.writeValueAsString(payload);
        synchronized (session) {
            if (session.isOpen()) session.sendMessage(new TextMessage(json));
        }
    }

    private String pendingKey(Long serverId, String requestId) {
        return serverId + ":" + requestId;
    }

    private void failPendingRequests(Long serverId, String message) {
        String prefix = serverId + ":";
        pendingRequests.forEach((key, client) -> {
            if (!key.startsWith(prefix) || !pendingRequests.remove(key, client)) return;
            try {
                if (client.isOpen()) sendError(client, key.substring(prefix.length()), message);
            } catch (IOException ignored) {
            }
        });
    }

    private String extractCookie(WebSocketSession session, String name) {
        var cookieHeaders = session.getHandshakeHeaders().get(HttpHeaders.COOKIE);
        if (cookieHeaders == null) return null;
        for (String header : cookieHeaders) {
            for (String cookie : header.split(";")) {
                String trimmed = cookie.trim();
                if (trimmed.startsWith(name + "=")) return trimmed.substring(name.length() + 1);
            }
        }
        return null;
    }

    private record ConnectionInfo(Long serverId, boolean plugin) {
    }
}
