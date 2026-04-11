package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.entity.MinecraftServerReports;
import me.xap3y.space.entity.TrSession;
import me.xap3y.space.repository.TrSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class TrSessionService {

    private final TrSessionRepository trSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public TrSession save(TrSession trSession) {
        return trSessionRepository.save(trSession);
    }

    public TrSession getByTokenStrict(String token) {
        return trSessionRepository.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("TrSession with token " + token + " not found"));
    }

    public TrSession getValidSession(String token) {
        TrSession session = getByTokenStrict(token);
        if (session.getIsValid() && session.getExpiresAt().isAfter(LocalDateTime.now())) {
            return session;
        } else {
            invalidateSession(session);
            throw new ResourceNotFoundException("TrSession with token " + token + " is invalid or expired");
        }
    }

    public TrSession getByToken(String token) {
        return trSessionRepository.findByToken(token).orElse(null);
    }

    public void invalidateSession(TrSession trSession) {
        trSession.setIsValid(false);
        trSessionRepository.save(trSession);
    }

    public TrSession createSession(MinecraftServerReports user, String userAgent, String ipAddress) {
        String token = UUID.randomUUID().toString();
        String sanitizedUserAgent = userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
        String secret = passwordEncoder.encode(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        TrSession session = new TrSession(secret, user, sanitizedUserAgent, ipAddress, expiresAt);
        return trSessionRepository.save(session);
    }

}
