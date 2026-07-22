package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.entity.Session;
import me.xap3y.space.entity.User;
import me.xap3y.space.repository.SessionRepository;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

        Session session = new Session(token, user, userAgent, ipAddress, expiresAt);
        sessionRepo.save(session);
        webhookService.postSessionInit(session);
        return token;
    }

    public List<Session> getSessions(Long userId) {
        return sessionRepo.findAllByUserIdAndIsValidTrue(userId);
    }

    @Nullable
    public Session getSession(String token) {
        return sessionRepo.findByToken(token).orElse(null);
    }

    @Nullable
    public Session getValidSession(String token) {
        return sessionRepo.findByToken(token)
                .filter(Session::getIsValid)
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    @Nullable
    public Session getInvalidSession(String token) {
        return sessionRepo.findByToken(token)
                .filter(s -> !s.getIsValid() || s.getExpiresAt().isBefore(LocalDateTime.now()))
                .orElse(null);
    }

    public Optional<Session> findById(Long id) {
        return sessionRepo.findById(id);
    }

    public void invalidateSession(String token) {
        sessionRepo.findByToken(token).ifPresent(session -> {
            session.setIsValid(false);
            sessionRepo.save(session);
            webhookService.postSessionInvalid(session);
        });
    }

    public void invalidateSessionById(Long id) {
        sessionRepo.findById(id).ifPresent(session -> {
            session.setIsValid(false);
            sessionRepo.save(session);
            webhookService.postSessionInvalid(session);
        });
    }

    public void invalidateAllSessionsForUser(User user) {
        List<Session> sessions = sessionRepo.findAllByUser(user);
        for (Session session : sessions) {
            session.setIsValid(false);
        }
        sessionRepo.saveAll(sessions);
    }
}
