package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByUniqueId(String uniqueId);

    List<Image> findAllByUploaderId(Long uploaderId);

    boolean existsByUniqueId(String uniqueId);

    long countByUploadTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT MAX(e.size) as largest, MIN(e.size) as smallest, AVG(e.size) as average, SUM(e.size) as total " +
            "FROM Image e " +
            "WHERE e.uploadTime BETWEEN :startDate AND :endDate")
    Object findTopSizesInRange(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.fileType, COUNT(e) as fileCount " +
            "FROM Image e " +
            "WHERE e.uploadTime BETWEEN :startDate AND :endDate " +
            "GROUP BY e.fileType " +
            "ORDER BY fileCount DESC")
    List<Object[]> findFileTypeLeaderboardInRange(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.uploader.id as uid, u.username, u.avatar, COUNT(e) as uploadCount " +
            "FROM Image e " +
            "INNER JOIN User u ON u.id = e.uploader.id " +
            "WHERE e.uploadTime BETWEEN :startDate AND :endDate " +
            "GROUP BY e.uploader.id " +
            "ORDER BY uploadCount DESC")
    List<Object[]> findBiggestUploaderInRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.uploader.id as uid, COUNT(e) as uploadCount, SUM(e.size) as totalSize " +
            "FROM Image e " +
            "WHERE e.uploadTime BETWEEN :startDate AND :endDate " +
            "AND e.uploader.id = :uid " +
            "GROUP BY e.uploader.id")
    Object findTopUserStats(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate,
                            @Param("uid") Long uid);


    @Transactional
    void deleteByUniqueId(String uniqueId);
}
