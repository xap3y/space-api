package me.xap3y.space.dto;

import java.time.LocalDateTime;

public record DefaultResponse(
        boolean error,
        String message,
        LocalDateTime timestamp
) {
}
