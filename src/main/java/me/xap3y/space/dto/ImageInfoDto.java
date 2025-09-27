package me.xap3y.space.dto;

import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.model.UserWebhookSettings;

import java.time.LocalDateTime;

public record ImageInfoDto(
        String uniqueId,
        String type,
        String description,
        long size,
        LocalDateTime uploadedAt,
        LocalDateTime expiresAt,
        UrlSetDto urlSet,
        ShortUserDto uploader,
        boolean requiresPassword,
        boolean isPublic,
        ImageLocation location,
        UserWebhookSettings webhookSettings
) {
}
