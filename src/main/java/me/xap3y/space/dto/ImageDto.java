package me.xap3y.space.dto;

import me.xap3y.space.entity.User;

import java.time.LocalDateTime;

public record ImageDto(
        byte[] bytes,
        User uploader,
        String description,
        String type,
        String password,
        long size,
        String base64,
        LocalDateTime uploadedAt,
        LocalDateTime expiresAt,
        boolean isPublic
) { }
