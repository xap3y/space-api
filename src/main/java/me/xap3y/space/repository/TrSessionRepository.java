package me.xap3y.space.repository;

import me.xap3y.space.entity.Session;
import me.xap3y.space.entity.TrSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrSessionRepository extends JpaRepository<TrSession, Long> {

    Optional<TrSession> findByToken(String token);

    Optional<TrSession> findByUser_Id(Long userId);

    List<Session> findAllByUser_Id(Long userId);

    long count();
}
