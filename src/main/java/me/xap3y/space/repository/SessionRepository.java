package me.xap3y.space.repository;

import me.xap3y.space.entity.Sessions;
import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Sessions, Long> {

    Optional<Sessions> findByToken(String token);

    List<Sessions> findAllByUserId(User userId);
}
