package me.xap3y.space.repository;

import me.xap3y.space.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyCode(String key);

    boolean existsByKeyCode(String key);
}
