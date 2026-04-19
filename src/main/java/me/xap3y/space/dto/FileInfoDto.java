package me.xap3y.space.dto;

import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.api.enums.ResourceSourceType;

import java.time.LocalDateTime;

public record FileInfoDto(
        String uniqueId,
        String fileName,
        String fileType,
        long size,
        String description,
        LocalDateTime uploadTime,
        LocalDateTime expirationTime,
        ImageLocation location,
        ResourceSourceType source
) {}