package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.entity.Session;
import me.xap3y.space.service.SessionService;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler that tracks every active frontend browser session.
 * Each browser tab opens /ws/active and keeps the connection alive.
 * Admin REST endpoints call this handler to list, send toasts, redirect,
 * close, or force-logout connected clients.
 */
@Slf4j
@Component
public class ActiveClientWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SessionService sessionService;

    /** Maps WS session ID -> rich metadata about the connected client */
    private final ConcurrentHashMap<String, ActiveClientInfo> clients = new ConcurrentHashMap<>();

    public ActiveClientWebSocketHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    // ------------------------------------------------------------------ //
    //  Lifecycle                                                           //
    // ------------------------------------------------------------------ //

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        var uri = session.getUri();
        String userAgent = session.getHandshakeHeaders().getFirst(HttpHeaders.USER_AGENT);
        if (userAgent == null) userAgent = "Unknown";

        // Resolve IP (support X-Forwarded-For passed as a query param or from headers)
        String ip = resolveIp(session);

        // Read cookies from handshake headers to extract session_token
        String authToken = null;
        java.util.List<String> cookieHeaders = session.getHandshakeHeaders().get(HttpHeaders.COOKIE);
        if (cookieHeaders != null) {
            for (String header : cookieHeaders) {
                String[] cookies = header.split(";");
                for (String cookie : cookies) {
                    if (cookie.trim().startsWith("session_token=")) {
                        authToken = cookie.trim().substring("session_token=".length());
                    }
                }
            }
        }

        // Optional: resolve logged-in user from the session token
        Long userId = null;
        String username = null;
        if (authToken != null && !authToken.isBlank()) {
            Session dbSession = sessionService.getValidSession(authToken);
            if (dbSession != null) {
                userId = dbSession.getUser().getId();
                username = dbSession.getUser().getUsername();
            }
        }

        // Initial page from query
        String page = "/";
        if (uri != null) {
            String p = Utils.extractQueryParam(uri.getQuery(), "page");
            if (p != null && !p.isBlank()) page = p;
        }

        ActiveClientInfo info = new ActiveClientInfo(
                session,
                userAgent,
                ip,
                Instant.now(),
                page,
                userId,
                username,
                authToken
        );

        clients.put(session.getId(), info);
        log.info("[ActiveWS] connected: sessionId={}, ip={}, user={}", session.getId(), ip, username);

        // Acknowledge connection
        sendJson(session, Map.of("type", "connected", "sessionId", session.getId()));
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        clients.remove(session.getId());
        log.info("[ActiveWS] disconnected: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) {
        // Clients may send page-update heartbeats: {"type":"pageUpdate","page":"/home/gallery"}
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            ActiveClientInfo info = clients.get(session.getId());
            if (info == null) return;

            if ("pageUpdate".equals(type)) {
                String newPage = (String) payload.get("page");
                if (newPage != null && !newPage.isBlank()) {
                    info.setCurrentPage(newPage);
                }
            } else if ("ping".equals(type)) {
                sendJson(session, Map.of("type", "pong"));
            }
        } catch (Exception e) {
            log.warn("[ActiveWS] Failed to parse message from {}: {}", session.getId(), e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Admin actions                                                       //
    // ------------------------------------------------------------------ //

    /** Returns a snapshot of all connected clients as serialisable DTOs */
    public java.util.List<ActiveClientDto> listClients() {
        return clients.values().stream()
                .map(ActiveClientInfo::toDto)
                .toList();
    }

    /** Send a toast notification to a specific WS session */
    public boolean sendToast(String wsSessionId, String message, String type, String position) {
        ActiveClientInfo info = clients.get(wsSessionId);
        if (info == null) return false;
        try {
            sendJson(info.getSession(), Map.of("type", "toast", "message", message, "toastType", type, "position", position));
            return true;
        } catch (IOException e) {
            log.warn("[ActiveWS] sendToast failed for {}: {}", wsSessionId, e.getMessage());
            return false;
        }
    }

    /** Redirect a specific WS session to another route */
    public boolean redirect(String wsSessionId, String route) {
        ActiveClientInfo info = clients.get(wsSessionId);
        if (info == null) return false;
        try {
            sendJson(info.getSession(), Map.of("type", "redirect", "route", route));
            return true;
        } catch (IOException e) {
            log.warn("[ActiveWS] redirect failed for {}: {}", wsSessionId, e.getMessage());
            return false;
        }
    }

    /** Force-close the WS connection */
    public boolean closeClient(String wsSessionId) {
        ActiveClientInfo info = clients.get(wsSessionId);
        if (info == null) return false;
        try {
            sendJson(info.getSession(), Map.of("type", "close", "reason", "Closed by admin"));
            info.getSession().close(new CloseStatus(4000, "Closed by admin"));
            clients.remove(wsSessionId);
            return true;
        } catch (IOException e) {
            log.warn("[ActiveWS] closeClient failed for {}: {}", wsSessionId, e.getMessage());
            return false;
        }
    }

    /** Force-logout: invalidate the linked auth session and close WS */
    public boolean logoutClient(String wsSessionId) {
        ActiveClientInfo info = clients.get(wsSessionId);
        if (info == null) return false;
        // Invalidate auth session if linked
        if (info.getUserId() != null && info.getAuthToken() != null) {
            sessionService.invalidateSession(info.getAuthToken());
        }
        try {
            sendJson(info.getSession(), Map.of("type", "logout", "reason", "Logged out by admin"));
            info.getSession().close(new CloseStatus(4001, "Logged out by admin"));
            clients.remove(wsSessionId);
        } catch (IOException e) {
            log.warn("[ActiveWS] logoutClient failed for {}: {}", wsSessionId, e.getMessage());
        }
        return true;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private String resolveIp(@NotNull WebSocketSession session) {
        HttpHeaders headers = session.getHandshakeHeaders();
        String xff = headers.getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = headers.getFirst("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        var remoteAddr = session.getRemoteAddress();
        return remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : "unknown";
    }

    private void sendJson(@NotNull WebSocketSession session, Object obj) throws IOException {
        if (!session.isOpen()) return;
        synchronized (session) {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(obj)));
        }
    }

    // ------------------------------------------------------------------ //
    //  Inner data classes                                                  //
    // ------------------------------------------------------------------ //

    public static class ActiveClientInfo {
        private final WebSocketSession session;
        private final String userAgent;
        private final String ip;
        private final Instant connectedAt;
        private volatile String currentPage;
        private final Long userId;
        private final String username;
        private final String authToken;

        public ActiveClientInfo(WebSocketSession session, String userAgent, String ip,
                                Instant connectedAt, String currentPage,
                                Long userId, String username, String authToken) {
            this.session = session;
            this.userAgent = userAgent;
            this.ip = ip;
            this.connectedAt = connectedAt;
            this.currentPage = currentPage;
            this.userId = userId;
            this.username = username;
            this.authToken = authToken;
        }

        public WebSocketSession getSession() { return session; }
        public String getUserAgent() { return userAgent; }
        public String getIp() { return ip; }
        public Instant getConnectedAt() { return connectedAt; }
        public String getCurrentPage() { return currentPage; }
        public void setCurrentPage(String p) { this.currentPage = p; }
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getAuthToken() { return authToken; }

        public ActiveClientDto toDto() {
            return new ActiveClientDto(
                    session.getId(),
                    userAgent,
                    ip,
                    connectedAt.toEpochMilli(),
                    currentPage,
                    userId,
                    username,
                    session.isOpen()
            );
        }
    }

    public record ActiveClientDto(
            String wsSessionId,
            String userAgent,
            String ip,
            long connectedAtMs,
            String currentPage,
            Long userId,
            String username,
            boolean connected
    ) {}
}
