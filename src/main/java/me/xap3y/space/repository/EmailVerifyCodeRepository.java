package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.EmailVerifyCodes;
import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerifyCodeRepository extends JpaRepository<EmailVerifyCodes, Long> {

    Optional<EmailVerifyCodes> findByEmail(String email);

    Optional<EmailVerifyCodes> findByCode(String code);

    Optional<EmailVerifyCodes> findByUrlCode(String code);

    Optional<EmailVerifyCodes> findByTelCode(String code);

    Optional<EmailVerifyCodes> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<EmailVerifyCodes> findTopByUserOrderByCreatedAtDesc(User user);

    boolean existsByEmail(String email);

    boolean existsByTelCode(String telCode);

    @Transactional
    void deleteByEmail(String email);
}
