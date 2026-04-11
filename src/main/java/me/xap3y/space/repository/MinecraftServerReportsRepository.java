package me.xap3y.space.repository;

import me.xap3y.space.entity.MinecraftServerReports;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MinecraftServerReportsRepository extends JpaRepository<MinecraftServerReports, Long> {

    Optional<MinecraftServerReports> findByApiKey(String apiKey);

    @NonNull List<MinecraftServerReports> findAll();

    Optional<MinecraftServerReports> findByOwnerEmail(String ownerEmail);

    Optional<MinecraftServerReports> findByServerName(String serverName);

    boolean existsByServerName(String serverName);

    boolean existsByOwnerEmail(String ownerEmail);
}
