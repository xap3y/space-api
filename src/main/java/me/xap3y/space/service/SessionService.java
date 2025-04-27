package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.entity.Sessions;
import me.xap3y.space.entity.User;
import me.xap3y.space.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SessionService {

    private final SessionRepository sessionRepo;
    private final WebhookService webhookService;

    public SessionService(SessionRepository sessionRepo, WebhookService webhookService) {
        this.sessionRepo = sessionRepo;
        this.webhookService = webhookService;
    }

    public String createSession(User user, String userAgent, String ipAddress) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        Sessions session = new Sessions(token, user, userAgent, ipAddress, expiresAt);
        sessionRepo.save(session);
        webhookService.postSessionInit(session);
        return token;
    }

    public Sessions getSession(String token) {
        return sessionRepo.findByToken(token)
                .filter(Sessions::getIsValid)
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    public void invalidateSession(String token) {
        sessionRepo.findByToken(token).ifPresent(session -> {
            session.setIsValid(false);
            sessionRepo.save(session);
            webhookService.postSessionInvalid(session);
        });
    }

    public void invalidateAllSessionsForUser(User user) {
        List<Sessions> sessions = sessionRepo.findAllByUserId(user);
        for (Sessions session : sessions) {
            session.setIsValid(false);
        }
        sessionRepo.saveAll(sessions);
    }
}
