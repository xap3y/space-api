package me.xap3y.space.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ImageAlbumDto(
    Long id,
    String uniqueId,
    String description,
    LocalDateTime createdAt,
    ShortUserDto owner,
    List<PlaylistImageDto> images
) {
}
