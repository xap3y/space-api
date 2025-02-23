package me.xap3y.space.mapper;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.Url;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class ShortUrlMapper implements Function<Url, ShortUrlDto> {

    private final ServerInfo serverInfo;
    private final ShortUserMapper shortUserMapper;

    public ShortUrlMapper(ServerInfo serverInfo, ShortUserMapper shortUserMapper) {
        this.serverInfo = serverInfo;
        this.shortUserMapper = shortUserMapper;
    }

    @Override
    public ShortUrlDto apply(Url url) {
        return new ShortUrlDto(
                url.getShortCode(),
                url.getOriginalUrl(),
                url.getVisits(),
                url.getMaxUses(),
                url.getCreatedAt(),
                url.getExpiresAt(),
                new UrlSetDto(
                        null,
                        serverInfo.getFrontEndUrl() + "/r0/" + url.getShortCode(),
                        serverInfo.getBaseUrl() + "/v1/url/r/" + url.getShortCode(),
                        serverInfo.getShortShortenerUrl() + "/" + url.getShortCode(),
                        null
                ),
                shortUserMapper.apply(url.getCreatedBy())
        );
    }
}
