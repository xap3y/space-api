package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record UrlDto(
        String url,
        String shortCode,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        int visits,
        String uploader
) {
}
