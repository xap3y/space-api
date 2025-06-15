package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.DiscordConnection;
import me.xap3y.space.entity.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscordConnectionRepository extends JpaRepository<DiscordConnection, Long> {

    Optional<DiscordConnection> findByDiscordId(String id);

    Optional<DiscordConnection> findByAccessToken(String id);

    @NotNull Optional<DiscordConnection> findById(@NotNull Long id);

    Optional<DiscordConnection> findByUserId(User id);


    boolean existsByUserId(User userId);

    boolean existsByDiscordId(String id);

    @Transactional
    void deleteByDiscordId(String id);

    @Transactional
    void deleteByAccessToken(String id);
}
