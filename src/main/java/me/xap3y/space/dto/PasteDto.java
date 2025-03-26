package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record PasteDto(
        String title,
        String content,
        boolean isPublic,
        LocalDateTime createdAt,
        String uniqueId,
        UrlSetDto urlSet,
        ShortUserDto uploader
) {
}
