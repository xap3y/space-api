package me.xap3y.space.mapper;

import me.xap3y.space.dto.LogDto;
import me.xap3y.space.entity.LogRecord;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class LogMapper implements Function<LogRecord, LogDto> {

    @Override
    public LogDto apply(LogRecord logRecord) {
        return new LogDto(
                logRecord.getIp(),
                logRecord.getUserAgent(),
                logRecord.getPath(),
                logRecord.getMethod(),
                logRecord.getResult()
        );
    }
}
