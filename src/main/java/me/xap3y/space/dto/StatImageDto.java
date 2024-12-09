package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record StatImageDto(
        String id,
        LocalDateTime uploadedAt,
        String type,
        long size,
        String url
) {
}
