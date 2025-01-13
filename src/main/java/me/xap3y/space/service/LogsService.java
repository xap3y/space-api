package me.xap3y.space.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.dto.LogDto;
import me.xap3y.space.entity.LogRecord;
import me.xap3y.space.repository.LogsRepository;
import me.xap3y.space.util.ConfigDb;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.time.LocalDateTime;

@Slf4j
@Service
public class LogsService {



    private final LogsRepository logsRepository;

    public LogsService(LogsRepository logsRepository) {
        this.logsRepository = logsRepository;
    }

    public void log(LogDto logDto) throws IllegalArgumentException, OptimisticLockingFailureException {
        LogRecord logRecord = new LogRecord();
        logRecord.setIp(logDto.ip());
        logRecord.setMethod(logDto.method());
        logRecord.setPath(logDto.path());
        logRecord.setTimestamp(LocalDateTime.now());
        logRecord.setUserAgent(logDto.userAgent());
        logRecord.setResult(logDto.result());
        logsRepository.save(logRecord);
    }

    @SneakyThrows
    public void logFile(String content) {
        FileWriter fw = new FileWriter(ConfigDb.LOG_FILE, true);
        fw.write(content + "\n");
        fw.close();
    }
}
