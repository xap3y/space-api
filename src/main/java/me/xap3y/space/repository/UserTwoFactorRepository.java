package me.xap3y.space.repository;

import me.xap3y.space.entity.UserTwoFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserTwoFactorRepository extends JpaRepository<UserTwoFactor, Long> {
    Optional<UserTwoFactor> findByUserId(Long userId);
}
