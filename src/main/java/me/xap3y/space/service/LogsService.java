package me.xap3y.space.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.dto.LogDto;
import me.xap3y.space.entity.LogRecord;
import me.xap3y.space.repository.LogsRepository;
import me.xap3y.space.util.ConfigDb;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.time.LocalDateTime;

@Slf4j
@Service
public class LogsService {

    private static final int DEFAULT_VARCHAR_LENGTH = 255;

    private final LogsRepository logsRepository;

    public LogsService(LogsRepository logsRepository) {
        this.logsRepository = logsRepository;
    }

    public void log(LogDto logDto) {
        try {
            LogRecord logRecord = new LogRecord();
            logRecord.setIp(limit(logDto.ip()));
            logRecord.setMethod(limit(logDto.method()));
            logRecord.setPath(limit(logDto.path()));
            logRecord.setTimestamp(LocalDateTime.now());
            logRecord.setUserAgent(limit(logDto.userAgent()));
            logRecord.setResult(limit(logDto.result()));
            logsRepository.save(logRecord);
        } catch (RuntimeException ex) {
            // Request telemetry must never cause the request itself to fail.
            log.warn("Failed to persist request log: {}", ex.getMessage());
        }
    }

    private String limit(String value) {
        if (value == null) return "";
        return value.length() <= DEFAULT_VARCHAR_LENGTH
                ? value
                : value.substring(0, DEFAULT_VARCHAR_LENGTH);
    }

    @SneakyThrows
    public void logFile(String content) {
        FileWriter fw = new FileWriter(ConfigDb.LOG_FILE, true);
        LocalDateTime now = LocalDateTime.now();
        fw.write(now + " " + content + "\n");
        fw.close();
    }
}
