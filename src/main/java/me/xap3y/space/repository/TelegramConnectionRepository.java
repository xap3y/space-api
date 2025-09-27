package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.TelegramConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramConnectionRepository extends JpaRepository<TelegramConnection, Long> {

    boolean existsByTelegramId(String telegramId);

    boolean existsByUserId_Id(Long userId);

    Optional<TelegramConnection> findByTelegramId(String telegramId);

    Optional<TelegramConnection> findByAccessToken(String accessToken);

    Optional<TelegramConnection> findByUserId_Id(Long userId);

    @Transactional
    void deleteByUserId_Id(Long userId);
}
