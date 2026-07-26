package me.xap3y.space.controller.admin;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.handler.ActiveClientWebSocketHandler;
import me.xap3y.space.model.response.DefaultResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API consumed exclusively by the admin frontend to manage
 * live browser sessions tracked via /ws/active.
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin/active-sessions")
@AllArgsConstructor
public class AdminActiveSessionController {

    private final ActiveClientWebSocketHandler activeClientHandler;

    /** List all currently connected browser sessions */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiresSpecialApiKey
    public ResponseEntity<?> listSessions() {
        List<ActiveClientWebSocketHandler.ActiveClientDto> sessions = activeClientHandler.listClients();
        return ResponseEntity.ok(new DefaultResponse(false, sessions, sessions.size()));
    }

    /** Send a toast message to a specific browser session */
    @PostMapping("/{wsSessionId}/toast")
    @RequiresSpecialApiKey
    public ResponseEntity<?> sendToast(
            @PathVariable String wsSessionId,
            @RequestBody Map<String, String> body
    ) {
        String message = body.getOrDefault("message", "Hello from admin!");
        String type = body.getOrDefault("type", "info");
        String position = body.getOrDefault("position", "top-right");
        boolean ok = activeClientHandler.sendToast(wsSessionId, message, type, position);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new DefaultResponse(false, "Toast sent", 1));
    }

    /** Redirect a specific browser session to another route */
    @PostMapping("/{wsSessionId}/redirect")
    @RequiresSpecialApiKey
    public ResponseEntity<?> redirectClient(
            @PathVariable String wsSessionId,
            @RequestBody Map<String, String> body
    ) {
        String route = body.getOrDefault("route", "/");
        boolean ok = activeClientHandler.redirect(wsSessionId, route);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new DefaultResponse(false, "Redirect sent", 1));
    }

    /** Force-close (disconnect) a browser session's WebSocket */
    @DeleteMapping("/{wsSessionId}")
    @RequiresSpecialApiKey
    public ResponseEntity<?> closeSession(@PathVariable String wsSessionId) {
        boolean ok = activeClientHandler.closeClient(wsSessionId);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new DefaultResponse(false, "Session closed", 1));
    }

    /** Force-logout: invalidate auth session + close WS */
    @PostMapping("/{wsSessionId}/logout")
    @RequiresSpecialApiKey
    public ResponseEntity<?> logoutSession(@PathVariable String wsSessionId) {
        boolean ok = activeClientHandler.logoutClient(wsSessionId);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new DefaultResponse(false, "Session logged out", 1));
    }
}
