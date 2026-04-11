package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.TempMailStatus;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.service.TempMailService;
import me.xap3y.space.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TempEmailWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final TempMailService tempMailService;

    public TempEmailWebSocketHandler(TempMailService tempMailService) {
        this.tempMailService = tempMailService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String email = Utils.extractQueryParam(uri.getQuery(), "email");
        if (email == null || email.isBlank()) {
            session.close(new CloseStatus(4001, "Missing email"));
            return;
        }

        TempMail mail = tempMailService.findByEmail(email).orElse(null);
        if (mail == null) {
            session.close(new CloseStatus(4002, "Email not found"));
            return;
        }else if (!mail.getStatus().equals(TempMailStatus.OPEN)) {
            session.close(new CloseStatus(4003, "Email not open"));
            return;
        }
        else if (mail.getExpireAt().isBefore(LocalDateTime.now())) {
            session.close(new CloseStatus(4003, "Email expired"));
            return;
        }

        boolean isCookieInvalid = true;
        HttpHeaders handshakeHeaders = session.getHandshakeHeaders();
        String cookieHeader = handshakeHeaders.getFirst("Cookie");
        log.info("Cookie header: {}", cookieHeader);

        if (cookieHeader != null) {
            String expectedCookieName = "email_token_" + mail.getFingerprint();
            if (cookieHeader.contains(expectedCookieName)) {
                isCookieInvalid = false;
            }
        }

        String apiKey = Utils.extractQueryParam(uri.getQuery(), "apiKey");

        boolean isKeyInvalid = apiKey == null || apiKey.isBlank();

        if (isCookieInvalid && isKeyInvalid) {
            sendJson(session, Map.of(
                    "error", true,
                    "message", "Unauthorized: Missing or invalid cookie and API key"
            ));
            session.close(new CloseStatus(4001, "Unauthorized"));
            return;
        }

        log.info("EMAIL WS: {} {}", email, apiKey);

        if (apiKey != null && !mail.getCreatedBy().getApiKey().getKeyCode().equals(apiKey)){
            session.close(new CloseStatus(4002, "Invalid API key"));
            return;
        }

        sessions.put(email, session);

        /*List<InboundEmail> newMails = inboundMailService.findTop20ByTempMailOrderBySentDateDesc(mail);

        if (newMails.isEmpty()) return;

        for (InboundEmail emailObj : newMails) {
            InboundEmailDto emailDto = new InboundEmailDto(emailObj);
            pushEmail(emailDto);
        }*/
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        sessions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
    }

    public void pushEmail(InboundEmailDto email) {

        var session = sessions.get(email.to);
        if (session == null || !session.isOpen()) return;

        try {
            sendJson(session, Map.of(
                    "messageId", email.messageId,
                    "from", email.from,
                    "to", email.to,
                    "subject", email.subject,
                    "content", email.text,
                    "html", email.html,
                    "date", email.date,
                    "envelope", email.envelope
            ));
            //session.close(CloseStatus.NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeByEmail(String email) {
        var session = sessions.get(email);
        if (session == null || !session.isOpen()) return;

        try {
            sendJson(session, Map.of(
                    "error", true,
                    "message", "Email suspended or deleted",
                    "close", true
            ));
            session.close(CloseStatus.NORMAL);
        } catch (IOException e) {
            log.warn("Cannnot close WS session for email {}: {}", email, e.getMessage());
        }
    }

    private void sendJson(WebSocketSession session, Object obj) throws IOException {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(obj)));
    }
}
