package me.xap3y.space.repository;

import me.xap3y.space.entity.DiscordReportTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscordReportTranscriptRepository extends JpaRepository<DiscordReportTranscript, Long> {

    long count();

    List<DiscordReportTranscript> findAllByUser_Id(Long user_id);

    Optional<DiscordReportTranscript> findByUniqueId(String uniqueId);
}
