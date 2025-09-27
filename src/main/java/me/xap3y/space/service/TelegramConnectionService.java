package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.entity.TelegramConnection;
import me.xap3y.space.repository.TelegramConnectionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class TelegramConnectionService {

    private final TelegramConnectionRepository telegramConnectionRepository;

    public boolean existsByTelegramId(String telegramId) {
        return telegramConnectionRepository.existsByTelegramId(telegramId);
    }

    public boolean existsByUserId(Long userId) {
        return telegramConnectionRepository.existsByUserId_Id(userId);
    }

    public TelegramConnection findByUserStrict(Long id) {
        return telegramConnectionRepository.findByUserId_Id(id)
                .orElseThrow(() -> new RuntimeException("Telegram connection not found for user ID: " + id));
    }

    public TelegramConnection findByTelegramIdStrict(String telegramId) {
        return telegramConnectionRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Telegram connection not found for Telegram ID: " + telegramId));
    }

    public Optional<TelegramConnection> findByTelegramId(String telegramId) {
        return telegramConnectionRepository.findByTelegramId(telegramId);
    }

    public TelegramConnection findByAccessTokenStrict(String accessToken) {
        return telegramConnectionRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Telegram connection not found for access token: " + accessToken));
    }

    public TelegramConnection save(TelegramConnection connection) {
        return telegramConnectionRepository.save(connection);
    }

    public void revokeByUserId(Long userId) {
        telegramConnectionRepository.deleteByUserId_Id(userId);
    }
}
