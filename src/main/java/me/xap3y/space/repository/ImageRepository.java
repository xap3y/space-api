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

    @Query("SELECT e FROM Image e " +
                    "WHERE e.uploader.id = :uploaderId " +
                    "AND e.uploadTime BETWEEN :from AND :to " +
                    "ORDER BY e.uploadTime DESC " +
                    "LIMIT :amount"
    )
    List<Image> findAllByUploaderIdBetween(@Param("uploaderId") Long uploaderId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           @Param("amount") Integer amount);

    List<Image> findAllByUploaderId(Long uploaderId);

    boolean existsByUniqueId(String uniqueId);

    @Query("SELECT COUNT(e.id) " +
            "FROM Image e " +
            "WHERE e.uploadTime BETWEEN :startDate AND :endDate ")
    long countByUploadTimeBetween(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(e.id) FROM Image e WHERE e.uploadTime >= :startDate")
    long countByUploadTimeAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(e.id) " +
            "FROM Image e " +
            "WHERE e.uploadTime BETWEEN :startDate AND :endDate " +
            "AND e.uploader.id = :uploaderId " +
            "ORDER BY e.uploadTime DESC")
    long countByUploadTimeBetweenAndUploaderId(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("uploaderId") Long uploaderId);

    // Accept start and end date, get total of images each day
    @Query("SELECT DATE(e.uploadTime) as date, COUNT(e.id) as count " +
            "FROM Image e " +
            "WHERE e.uploadTime BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(e.uploadTime) " +
            "ORDER BY DATE(e.uploadTime) ASC")
    List<Object[]> findTotalImagesPerDay(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Accept start and end date, get total of images each day filter by user
    @Query("SELECT DATE(e.uploadTime) as date, COUNT(e.id) as count " +
            "FROM Image e " +
            "WHERE e.uploadTime BETWEEN :startDate AND :endDate " +
            "AND e.uploader.id = :uploaderId " +
            "GROUP BY DATE(e.uploadTime) " +
            "ORDER BY DATE(e.uploadTime) ASC")
    List<Object[]> findTotalImagesPerDayByUser(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                @Param("uploaderId") Long uploaderId);

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

    long count();

    @Transactional
    void deleteByUniqueId(String uniqueId);
}
