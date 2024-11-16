package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record PasteDto(
        String content,
        boolean isPublic,
        LocalDateTime createdAt,
        String uniqueId,
        String uploader
) {
}
