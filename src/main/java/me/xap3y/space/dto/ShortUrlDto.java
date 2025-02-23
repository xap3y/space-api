package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record ShortUrlDto (
        String uniqueId,
        String originalUrl,
        int visits,
        int maxUses,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        UrlSetDto urlSet,
        ShortUserDto uploader

) {
}
