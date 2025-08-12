package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.TempMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TempMailRepository extends JpaRepository<TempMail, Long> {

    Optional<TempMail> findByEmail(String email);

    boolean existsByEmail(String email);

    @Transactional
    void deleteByEmail(String email);
}
