package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long>, JpaSpecificationExecutor<Url> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Url> findByCreatedById(Long createdById);

    int countAllByCreatedById(Long createdById);

    @Transactional
    void deleteByShortCode(String shortCode);

    long count();

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Accept start and end date, get total of url each day filter by user
    @Query("SELECT DATE(e.createdAt) as date, COUNT(e.id) as count " +
            "FROM Url e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "AND e.createdBy.id = :uploaderId " +
            "GROUP BY DATE(e.createdAt) " +
            "ORDER BY DATE(e.createdAt) ASC")
    List<Object[]> findTotalUrlsPerDayByUser(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("uploaderId") Long uploaderId);

    // Get total biggest paste cretor in range, only get the id make return Optional<List<Object[]>>
    @Query("SELECT e.createdBy.id as uid, COUNT(e) as uploadCount " +
            "FROM Url e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "GROUP BY e.createdBy.id " +
            "ORDER BY uploadCount DESC LIMIT 1")
    Optional<List<Object[]>> findBiggestCreatorInRangeWithId(@Param("startDate") LocalDateTime startDate,
                                                               @Param("endDate") LocalDateTime endDate);
}
