package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record PasteSummary(
        Long id,
        String uniqueId,
        String title,
        LocalDateTime createdAt,
        Long createdById
) {}
