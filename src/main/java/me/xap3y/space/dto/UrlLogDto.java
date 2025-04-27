package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record UrlLogDto(
        ShortUrlDto shortUrl,
        String userAgent,
        String ipAddress,
        LocalDateTime time
) {
}
