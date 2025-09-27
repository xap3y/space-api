package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.entity.TelegramConnectCodes;
import me.xap3y.space.repository.TelegramConnectCodesRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TelegramConnectCodesService {

    private final TelegramConnectCodesRepository telegramConnectCodesRepository;

    public Optional<TelegramConnectCodes> findByCode(String code) {
        return telegramConnectCodesRepository.findByCode(code);
    }

    public Optional<TelegramConnectCodes> findByCodeNotUsed(String code) {
        return telegramConnectCodesRepository.findByCode(code)
                .filter(c -> !c.isUsed())
                .filter(c -> c.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    public List<TelegramConnectCodes> findByUserId(Long userId) {
        return telegramConnectCodesRepository.findByUser_Id(userId);
    }

    public Optional<TelegramConnectCodes> findByUserIdNotUsed(Long userId) {
        return telegramConnectCodesRepository.findByUser_Id(userId).stream()
                .filter(code -> !code.isUsed())
                .filter(code -> code.getExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst();
    }

    public void deleteByCode(String code) {
        telegramConnectCodesRepository.deleteByCode(code);
    }

    public void setCodeUsed(TelegramConnectCodes c) {
        c.setUsed(true);
        c.setUsedAt(LocalDateTime.now());
        telegramConnectCodesRepository.save(c);
    }

    public TelegramConnectCodes save(TelegramConnectCodes code) {
        return telegramConnectCodesRepository.save(code);
    }
}
