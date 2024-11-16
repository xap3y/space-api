package me.xap3y.space.dto;

public record LogDto(
        String ip,
        String userAgent,
        String path,
        String method,
        String result
) {
}
