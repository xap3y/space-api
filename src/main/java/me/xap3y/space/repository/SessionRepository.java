package me.xap3y.space.repository;

import me.xap3y.space.entity.Session;
import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByToken(String token);

    List<Session> findAllByUser(User userId);

    List<Session> findAllByUserIdAndIsValidTrue(Long userId);
}
