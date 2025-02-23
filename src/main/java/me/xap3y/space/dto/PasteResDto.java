package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record PasteResDto (
        String title,
        String content,
        boolean isPublic,
        String uniqueId,
        LocalDateTime createdAt,
        ShortUserDto uploader,
        UrlSetDto urlSet
) {
}
