package me.xap3y.space.dto;

public record TranscriptImageDto(
        String uniqueId,
        String fileType,
        long size,
        UrlSetDto urlSet
) {
}
