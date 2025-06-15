package me.xap3y.space.dto;

import me.xap3y.space.model.UserWebhookSettings;

public record UserSettingsDto(
        long uid,
        String username,
        UserWebhookSettings webhookSettings
) {
}
