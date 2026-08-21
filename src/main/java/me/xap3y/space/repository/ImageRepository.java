package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long>, JpaSpecificationExecutor<Image> {

    Optional<Image> findByUniqueId(String uniqueId);

    /*@Query("SELECT e FROM Image e " +
                    "WHERE e.uploader.id = :uploaderId " +
                    "AND e.uploadTime >= :from AND e.uploadTime <= :to " +
                    "ORDER BY e.uploadTime DESC " +
                    "LIMIT :amount"
    )
    List<Image> findAllByUploaderIdBetween(
            @Param("uploaderId") Long uploaderId,
           @Param("from") LocalDateTime from,
           @Param("to") LocalDateTime to,
           @Param("amount") Integer amount
    );*/

    @Query("SELECT e FROM Image e " +
            "WHERE e.uploader.id = :uploaderId " +
            "AND e.uploadTime >= :from AND e.uploadTime <= :to " +
            "ORDER BY e.uploadTime DESC " +
            "LIMIT :amount"
    )
    List<Image> findAllByUploaderIdBetween(@Param("uploaderId") Long uploaderId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           @Param("amount") Integer amount);

    Page<Image> findByUploaderId(Long uploaderId, Pageable pageable);

    @Query("SELECT e FROM Image e WHERE e.fileType IN :fileTypes")
    List<Image> findAllByFileTypeIn(Set<String> fileTypes);

    @Query("SELECT e FROM Image e " +
            "WHERE e.uploader.id = :uploaderId " +
            "AND e.uploadTime < :before " +
            "ORDER BY e.uploadTime DESC " +
            "LIMIT :limit"
    )
    List<Image> findAllByUploaderIdBefore(@Param("uploaderId") Long uploaderId,
                                           @Param("before") LocalDateTime to,
                                           @Param("limit") Integer limit);

    int countByUploaderId(Long uploaderId);

    List<Image> findAllByUploaderId(Long uploaderId);

    boolean existsByUniqueId(String uniqueId);

    @Query("SELECT COUNT(e.id) " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate ")
    long countByUploadTimeBetween(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(e.size) " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate")
    Long sumByUploadTimeBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.uploader.id as uid, COUNT(e) as uploadCount " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "GROUP BY e.uploader.id " +
            "ORDER BY uploadCount DESC LIMIT 1")
    Optional<List<Object[]>> findBestUploader(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(e.id) FROM Image e WHERE e.uploadTime >= :startDate")
    long countByUploadTimeAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(e.id) " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId " +
            "ORDER BY e.uploadTime DESC")
    long countByUploadTimeBetweenAndUploaderId(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("uploaderId") Long uploaderId);

    // Accept start and end date, get total of images each day
    @Query("SELECT DATE(e.uploadTime) as date, COUNT(e.id) as count " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "GROUP BY DATE(e.uploadTime) " +
            "ORDER BY DATE(e.uploadTime) ASC")
    List<Object[]> findTotalImagesPerDay(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Accept start and end date, get total of images each day filter by user
    @Query("SELECT DATE(e.uploadTime) as date, COUNT(e.id) as count " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId " +
            "GROUP BY DATE(e.uploadTime) " +
            "ORDER BY DATE(e.uploadTime) ASC")
    List<Object[]> findTotalImagesPerDayByUser(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                @Param("uploaderId") Long uploaderId);

    @Query("SELECT DATE(e.uploadTime), SUM(e.size) FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId GROUP BY DATE(e.uploadTime) ORDER BY DATE(e.uploadTime) ASC")
    List<Object[]> findStoragePerDayByUser(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("uploaderId") Long uploaderId);

    @Query("SELECT COALESCE(SUM(e.size), 0) FROM Image e WHERE e.uploader.id = :uploaderId")
    Long sumStorageByUploaderId(@Param("uploaderId") Long uploaderId);

    @Query("SELECT COALESCE(SUM(e.size), 0) FROM Image e " +
            "WHERE e.uploader.id = :uploaderId AND e.uploadTime < :before")
    Long sumStorageByUploaderIdBefore(@Param("uploaderId") Long uploaderId,
                                       @Param("before") LocalDateTime before);

    @Query("SELECT e.fileType, COUNT(e.id), SUM(e.size) FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId GROUP BY e.fileType ORDER BY COUNT(e.id) DESC")
    List<Object[]> findFileTypesByUser(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("uploaderId") Long uploaderId);

    @Query("SELECT e.location, COUNT(e.id), SUM(e.size) FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId GROUP BY e.location ORDER BY COUNT(e.id) DESC")
    List<Object[]> findLocationsByUser(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("uploaderId") Long uploaderId);

    @Query("SELECT e.isPublic, COUNT(e.id) FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId GROUP BY e.isPublic")
    List<Object[]> findVisibilityByUser(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         @Param("uploaderId") Long uploaderId);

    @Query("SELECT MAX(e.size) as largest, MIN(e.size) as smallest, AVG(e.size) as average, SUM(e.size) as total " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate")
    Object findTopSizesInRange(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.fileType, COUNT(e) as fileCount " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "GROUP BY e.fileType " +
            "ORDER BY fileCount DESC")
    List<Object[]> findFileTypeLeaderboardInRange(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.uploader.id as uid, u.username, u.avatar, COUNT(e) as uploadCount " +
            "FROM Image e " +
            "INNER JOIN User u ON u.id = e.uploader.id " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "GROUP BY e.uploader.id " +
            "ORDER BY uploadCount DESC")
    List<Object[]> findBiggestUploaderInRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.uploader.id as uid, COUNT(e) as uploadCount, SUM(e.size) as totalSize " +
            "FROM Image e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uid " +
            "GROUP BY e.uploader.id")
    Object findTopUserStats(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate,
                            @Param("uid") Long uid);

    long count();

    @Transactional
    void deleteByUniqueId(String uniqueId);
}
