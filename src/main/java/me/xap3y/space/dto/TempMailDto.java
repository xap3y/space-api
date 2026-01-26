package me.xap3y.space.dto;

import me.xap3y.space.api.enums.TempMailStatus;

import java.time.LocalDateTime;

public record TempMailDto(
        Long id,
        String email,
        TempMailStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        ShortUserDto createdBy
) {
}
