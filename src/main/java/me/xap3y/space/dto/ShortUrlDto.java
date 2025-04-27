package me.xap3y.space.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ShortUrlDto (
        String uniqueId,
        String originalUrl,
        int visits,
        int maxUses,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        UrlSetDto urlSet,
        ShortUserDto uploader,
        List<UrlLogDto> urlLogs
) {
}
