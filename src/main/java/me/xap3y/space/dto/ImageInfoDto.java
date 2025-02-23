package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record ImageInfoDto(
        String uniqueId,
        String type,
        long size,
        LocalDateTime uploadedAt,
        UrlSetDto urlSet,
        ShortUserDto uploader
) {
}
