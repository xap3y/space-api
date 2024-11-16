package me.xap3y.space.repository;

import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByApiKey(String apiKey);

    Optional<User> findByUsername(String username);
}
