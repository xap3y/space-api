package me.xap3y.space.mapper;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.Url;
import me.xap3y.space.service.UrlLogsService;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class ShortUrlMapper implements Function<Url, ShortUrlDto> {

    private final ServerInfo serverInfo;
    private final ShortUserMapper shortUserMapper;
    private final UrlLogsService urlLogsService;
    private final UrlSetMapper urlSetMapper;

    public ShortUrlMapper(ServerInfo serverInfo, ShortUserMapper shortUserMapper, UrlLogsService urlLogsService, UrlSetMapper urlSetMapper) {
        this.serverInfo = serverInfo;
        this.shortUserMapper = shortUserMapper;
        this.urlLogsService = urlLogsService;
        this.urlSetMapper = urlSetMapper;
    }

    @Override
    public ShortUrlDto apply(Url url) {
        return applyWithLogs(url, false);
    }

    public ShortUrlDto applyWithLogs(Url url, boolean logs) {
        return new ShortUrlDto(
                url.getShortCode(),
                url.getOriginalUrl(),
                url.getVisits(),
                url.getMaxUses(),
                url.getCreatedAt(),
                url.getExpiresAt(),
                urlSetMapper.apply(url),
                shortUserMapper.apply(url.getCreatedBy()),
                logs ? urlLogsService.getByUrlId(url.getId()) : null
        );
    }

}
