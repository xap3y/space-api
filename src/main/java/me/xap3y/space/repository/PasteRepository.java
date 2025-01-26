package me.xap3y.space.repository;

import me.xap3y.space.entity.Paste;
import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasteRepository extends JpaRepository<Paste, Long> {

    Optional<Paste> findByUniqueId(String uniqueId);

    List<Paste> findByCreatedById(Long createdById);

    List<Paste> findByCreatedBy(User user);

    void deleteByUniqueId(String uniqueId);

    int countAllByCreatedById(Long createdById);

    @Query("SELECT e.createdBy.id as uid, u.username, u.avatar, COUNT(e) as uploadCount " +
            "FROM Paste e " +
            "INNER JOIN User u ON u.id = e.createdBy.id " +
            "WHERE e.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY e.createdBy.id " +
            "ORDER BY uploadCount DESC")
    List<Object[]> findBiggestCreatorInRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    long count();

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
