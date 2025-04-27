package me.xap3y.space.mapper;

import lombok.AllArgsConstructor;
import me.xap3y.space.dto.UrlLogDto;
import me.xap3y.space.entity.UrlLogs;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@AllArgsConstructor
public class UrlLogMapper implements Function<UrlLogs, UrlLogDto> {

    @Override
    public UrlLogDto apply(UrlLogs urlLogs) {
        return new UrlLogDto(
                null,
                urlLogs.getUserAgent(),
                urlLogs.getIpAddress(),
                urlLogs.getTime()
        );
    }
}
