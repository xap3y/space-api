package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.dto.PasteSummary;
import me.xap3y.space.entity.Paste;
import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasteRepository extends JpaRepository<Paste, Long>, JpaSpecificationExecutor<Paste> {

    Optional<Paste> findByUniqueId(String uniqueId);

    List<Paste> findByCreatedById(Long createdById);

    List<Paste> findByCreatedBy(User user);

    boolean existsByUniqueId(String uid);

    @Transactional
    void deleteByUniqueId(String uniqueId);

    int countAllByCreatedById(Long createdById);

    @Query("SELECT e.createdBy.id as uid, u.username, u.avatar, COUNT(e) as uploadCount " +
            "FROM Paste e " +
            "INNER JOIN User u ON u.id = e.createdBy.id " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "GROUP BY e.createdBy.id " +
            "ORDER BY uploadCount DESC")
    List<Object[]> findBiggestCreatorInRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Get total biggest paste cretor in range, only get the id make return Optional<List<Object[]>>
    @Query("SELECT e.createdBy.id as uid, COUNT(e) as uploadCount " +
            "FROM Paste e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "GROUP BY e.createdBy.id " +
            "ORDER BY uploadCount DESC LIMIT 1")
    Optional<List<Object[]>> findBiggestCreatorInRangeWithId(@Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);

    long count();

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(e.id) " +
            "FROM Paste e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "AND e.createdBy.id = :createdBy")
    long countByCreatedAtBetweenAndCreatedById(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("createdBy") Long createdById);

    // Accept start and end date, get total of images each day
    @Query("SELECT DATE(e.createdAt) as date, COUNT(e.id) as count " +
            "FROM Paste e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "GROUP BY DATE(e.createdAt) " +
            "ORDER BY DATE(e.createdAt) ASC")
    List<Object[]> findTotalPastesPerDay(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Accept start and end date, get total of images each day filter by user
    @Query("SELECT DATE(e.createdAt) as date, COUNT(e.id) as count " +
            "FROM Paste e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "AND e.createdBy.id = :createdById " +
            "GROUP BY DATE(e.createdAt) " +
            "ORDER BY DATE(e.createdAt) ASC")
    List<Object[]> findTotalPastesPerDayByUser(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                @Param("createdById") Long createdById);
}
