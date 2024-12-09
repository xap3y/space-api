package me.xap3y.space.repository;

import me.xap3y.space.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String key);

    boolean existsByKeyHash(String key);
}
