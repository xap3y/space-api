package me.xap3y.space.model.response;

import java.time.LocalDateTime;

public record PackInfoResponse(
        String packId,
        String description,
        Integer totalFiles,
        Long totalSize,
        LocalDateTime uploadTime,
        Boolean isPasswordProtected,
        Boolean hasAccess
) {}