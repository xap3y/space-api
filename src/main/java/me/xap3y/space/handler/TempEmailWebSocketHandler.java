package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.InboundEmail;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.service.InboundMailService;
import me.xap3y.space.service.TempMailService;
import me.xap3y.space.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TempEmailWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final TempMailService tempMailService;
    private final InboundMailService inboundMailService;

    public TempEmailWebSocketHandler(TempMailService tempMailService, InboundMailService inboundMailService) {
        this.tempMailService = tempMailService;
        this.inboundMailService = inboundMailService;
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

        String apiKey = Utils.extractQueryParam(uri.getQuery(), "apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            session.close(new CloseStatus(4001, "Missing apiKey"));
            return;
        }

        log.info("EMAIL WS: {} {}", email, apiKey);

        // decode from URL encoding, dont use any library for this like Utils.decodeURIComponent


        TempMail mail = tempMailService.findByEmail(email).orElse(null);
        if (mail == null) {
            session.close(new CloseStatus(4002, "Email not found"));
            return;
        }

        if (mail.getExpireAt().isBefore(LocalDateTime.now())) {
            session.close(new CloseStatus(4003, "Email expired"));
            return;
        }

        if (!mail.getCreatedBy().getApiKey().getKeyCode().equals(apiKey)){
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

    private void sendJson(WebSocketSession session, Object obj) throws IOException {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(obj)));
    }
}
