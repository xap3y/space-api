package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Url> findByCreatedById(Long createdById);

    int countAllByCreatedById(Long createdById);

    @Transactional
    void deleteByShortCode(String shortCode);

    long count();

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
