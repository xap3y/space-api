package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record ImageInfoDto(
        String uniqueId,
        String type,
        String description,
        long size,
        LocalDateTime uploadedAt,
        LocalDateTime expiresAt,
        UrlSetDto urlSet,
        ShortUserDto uploader,
        boolean requiresPassword,
        boolean isPublic
) {
}
