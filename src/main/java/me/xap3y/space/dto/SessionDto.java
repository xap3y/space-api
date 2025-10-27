package me.xap3y.space.dto;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public record SessionDto(
        Long id,
        @Nullable ShortUserDto user,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        LocalDateTime expiresAt,
        Boolean isValid,
        String userAgent,
        String ipAddress
) {
}
