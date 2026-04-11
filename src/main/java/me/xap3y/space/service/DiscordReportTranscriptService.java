package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.entity.DiscordReportTranscript;
import me.xap3y.space.repository.DiscordReportTranscriptRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class DiscordReportTranscriptService {

    private final DiscordReportTranscriptRepository discordReportTranscriptRepository;

    public long count() {
        return discordReportTranscriptRepository.count();
    }

    public DiscordReportTranscript save(DiscordReportTranscript transcript) {
        return discordReportTranscriptRepository.save(transcript);
    }

    public Optional<DiscordReportTranscript> findByUniqueId(String uniqueId) {
        return discordReportTranscriptRepository.findByUniqueId(uniqueId);
    }

    public List<DiscordReportTranscript> findAllByUserId(Long userId) {
        return discordReportTranscriptRepository.findAllByUser_Id(userId);
    }
}
