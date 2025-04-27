package me.xap3y.space.repository;

import me.xap3y.space.entity.Url;
import me.xap3y.space.entity.UrlLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlLogsRepository extends JpaRepository<UrlLogs, Long> {

    Optional<UrlLogs> findById(Long id);

    Optional<List<UrlLogs>> findAllByUrl(Url urlId);

    int countByUrl(Url urlId);

    int countByIpAddress(String ipAddress);

    @Query("SELECT e FROM UrlLogs e WHERE e.url.id = :id")
    Optional<List<UrlLogs>> findByShortUrlId(Long id);

    @Query("SELECT e FROM UrlLogs e WHERE e.url.shortCode = :uid")
    Optional<List<UrlLogs>> findByShortUrlUniqueId(String uid);

    @Query("SELECT e FROM UrlLogs e WHERE e.url.id = :id AND e.ipAddress = :ipAddress")
    Optional<List<UrlLogs>> findByShortUrlIdAndIpAddress(Long id, String ipAddress);

    @Query("SELECT e FROM UrlLogs e WHERE e.url.id = :id AND e.userAgent = :userAgent")
    Optional<List<UrlLogs>> findByShortUrlIdAndUserAgent(Long id, String userAgent);
}
