package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record PlaylistImageDto(
        String uniqueId,
        String type,
        String description,
        long size,
        LocalDateTime uploadedAt,
        UrlSetDto urlSet,
        LowUserDto uploader
) {
}
