package me.xap3y.space.repository;

import me.xap3y.space.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByUniqueId(String uniqueId);

    Long countByUploadPackId(Long uploadPackId);

    @Query("SELECT COUNT(e.id) FROM FileEntity e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate AND e.uploader.id = :uploaderId")
    long countByUploaderIdInRange(@Param("uploaderId") Long uploaderId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(e.size), 0) FROM FileEntity e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate AND e.uploader.id = :uploaderId")
    Long sumStorageByUploaderIdInRange(@Param("uploaderId") Long uploaderId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DATE(e.uploadTime), COUNT(e.id) FROM FileEntity e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId GROUP BY DATE(e.uploadTime) ORDER BY DATE(e.uploadTime) ASC")
    List<Object[]> findTotalFilesPerDayByUser(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("uploaderId") Long uploaderId);

    @Query("SELECT DATE(e.uploadTime), SUM(e.size) FROM FileEntity e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId GROUP BY DATE(e.uploadTime) ORDER BY DATE(e.uploadTime) ASC")
    List<Object[]> findStoragePerDayByUser(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("uploaderId") Long uploaderId);

    @Query("SELECT COALESCE(SUM(e.size), 0) FROM FileEntity e WHERE e.uploader.id = :uploaderId")
    Long sumStorageByUploaderId(@Param("uploaderId") Long uploaderId);

    @Query("SELECT COALESCE(SUM(e.size), 0) FROM FileEntity e " +
            "WHERE e.uploader.id = :uploaderId AND e.uploadTime < :before")
    Long sumStorageByUploaderIdBefore(@Param("uploaderId") Long uploaderId,
                                       @Param("before") LocalDateTime before);

    @Query("SELECT e.fileType, COUNT(e.id), SUM(e.size) FROM FileEntity e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId GROUP BY e.fileType ORDER BY COUNT(e.id) DESC")
    List<Object[]> findFileTypesByUser(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("uploaderId") Long uploaderId);

    @Query("SELECT e.location, COUNT(e.id), SUM(e.size) FROM FileEntity e " +
            "WHERE e.uploadTime >= :startDate AND e.uploadTime <= :endDate " +
            "AND e.uploader.id = :uploaderId GROUP BY e.location ORDER BY COUNT(e.id) DESC")
    List<Object[]> findLocationsByUser(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("uploaderId") Long uploaderId);
}
