package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.User;
import me.xap3y.space.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    @Query("SELECT us FROM UserSettings us WHERE us.userId.id = :userId")
    Optional<UserSettings> findByUserId(Long userId);

    @Query("SELECT COUNT(us) > 0 FROM UserSettings us WHERE us.userId.id = :userId")
    boolean existsByUserId(Long userId);

    @Transactional
    void deleteByUserId(User userId);

    long count();
}
