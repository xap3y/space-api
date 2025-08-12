package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.xap3y.space.entity.EmailVerifyCodes;
import me.xap3y.space.service.EmailVerifyCodeService;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerifyWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final EmailVerifyCodeService tokenRepo;
    private final EmailVerifyCodeService emailVerifyCodeService;

    public VerifyWebSocketHandler(EmailVerifyCodeService service, EmailVerifyCodeService emailVerifyCodeService) {
        this.tokenRepo = service;
        this.emailVerifyCodeService = emailVerifyCodeService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        String telCode = Utils.extractQueryParam(uri.getQuery(), "telCode");
        if (telCode == null || telCode.isBlank()) {
            session.close(new CloseStatus(4001, "Missing telCode"));
            return;
        }

        var opt = tokenRepo.findByTelCodeStrict(telCode);
        if (opt.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.close(new CloseStatus(4003, "Expired"));
            return;
        }

        sessions.put(telCode, session);
        sendJson(session, Map.of("type", "channel_ready"));

        if (opt.isUsed()) {
            pushVerified(telCode);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
    }

    public void pushVerified(String telCode) {
        var session = sessions.get(telCode);
        if (session == null || !session.isOpen()) return;

        EmailVerifyCodes verifyCode = emailVerifyCodeService.findByTelCodeStrict(telCode);

        try {
            sendJson(session, Map.of(
                    "type", "verified",
                    "redirect", "/login?email=" + URLDecoder.decode(verifyCode.getEmail(), StandardCharsets.UTF_8)
            ));
            session.close(CloseStatus.NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendJson(WebSocketSession session, Object obj) throws IOException {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(obj)));
    }
}
