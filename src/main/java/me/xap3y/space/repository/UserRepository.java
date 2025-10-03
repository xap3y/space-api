package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.ApiKey;
import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByApiKey(ApiKey apiKey);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Transactional
    void deleteByUsername(String username);

    @Transactional
    void deleteById(Long id);
}

