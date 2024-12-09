package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record InviteCodeDto(
        String code,
        boolean used,
        LocalDateTime createdAt,
        LocalDateTime usedAt,
        Long createdBy,
        Long usedBy
) {
}
