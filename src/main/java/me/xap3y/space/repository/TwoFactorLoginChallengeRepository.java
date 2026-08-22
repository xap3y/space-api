package me.xap3y.space.repository;

import jakarta.persistence.LockModeType;
import me.xap3y.space.entity.TwoFactorLoginChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface TwoFactorLoginChallengeRepository extends JpaRepository<TwoFactorLoginChallenge, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TwoFactorLoginChallenge> findByTokenHashAndUsedFalse(String tokenHash);
}
