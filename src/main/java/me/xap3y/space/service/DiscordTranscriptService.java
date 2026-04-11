package me.xap3y.space.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.xap3y.space.model.DiscordTranscript;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DiscordTranscriptService {

    private final static String TRANSCRIPT_DIR = "transcripts/";

    private final ObjectMapper objectMapper;

    public DiscordTranscriptService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void saveToFile(DiscordTranscript transcript, String uniqueId) {
        String json;

        Path transcriptDirPath = Paths.get(TRANSCRIPT_DIR);
        if (!Files.exists(transcriptDirPath)) {
            try {
                Files.createDirectories(transcriptDirPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create transcript directory", e);
            }
        }

        try {
            json = objectMapper.writeValueAsString(transcript);
            Files.writeString(Paths.get(TRANSCRIPT_DIR + uniqueId + ".json"), json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save transcript to file", e);
        }
    }

    @Nullable
    public DiscordTranscript loadFromFile(String uniqueId) {
        try {
            Path path = Paths.get(TRANSCRIPT_DIR + uniqueId + ".json");

            boolean exists = Files.exists(path);
            if (!exists) {
                return null;
            }
            String json = Files.readString(path);
            return objectMapper.readValue(json, DiscordTranscript.class);
        } catch (Exception e) {
            return null;
        }
    }
}
