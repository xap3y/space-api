package me.xap3y.space.mapper;

import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.entity.Url;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UrlMapper implements Function<Url, UrlDto> {

    @Override
    public UrlDto apply(Url url) {
        return new UrlDto(
                url.getOriginalUrl(),
                url.getShortCode(),
                url.getCreatedAt(),
                url.getExpiresAt(),
                url.getVisits(),
                url.getCreatedBy().getUsername(),
                url.getMaxUses()
        );
    }
}
