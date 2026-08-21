package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.TempMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TempMailRepository extends JpaRepository<TempMail, Long> {

    Optional<TempMail> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(e.id) FROM TempMail e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate AND e.createdBy.id = :createdById")
    long countByUserInRange(@Param("createdById") Long createdById,
                            @Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DATE(e.createdAt), COUNT(e.id) FROM TempMail e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "AND e.createdBy.id = :createdById GROUP BY DATE(e.createdAt) ORDER BY DATE(e.createdAt) ASC")
    List<Object[]> findTotalPerDayByUser(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("createdById") Long createdById);

    @Query("SELECT e.status, COUNT(e.id) FROM TempMail e " +
            "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
            "AND e.createdBy.id = :createdById GROUP BY e.status ORDER BY COUNT(e.id) DESC")
    List<Object[]> findStatusesByUser(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("createdById") Long createdById);

    @Transactional
    void deleteByEmail(String email);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TempMail t SET t.status = me.xap3y.space.api.enums.TempMailStatus.SUSPENDED WHERE t.email = :email")
    void suspendEmail(@Param("email") String email);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TempMail t SET t.status = me.xap3y.space.api.enums.TempMailStatus.CLOSED WHERE t.email = :email")
    void closeEmail(@Param("email") String email);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TempMail t SET t.status = me.xap3y.space.api.enums.TempMailStatus.DELETED WHERE t.email = :email")
    void deleteEmail(@Param("email") String email);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TempMail t SET t.status = me.xap3y.space.api.enums.TempMailStatus.OPEN WHERE t.email = :email")
    void openMail(@Param("email") String email);
}
