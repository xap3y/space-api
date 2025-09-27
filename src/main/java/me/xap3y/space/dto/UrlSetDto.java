package me.xap3y.space.dto;

import org.springframework.lang.Nullable;


public record UrlSetDto(
        String webUrl,
        String portalUrl,
        String rawUrl,

        @Nullable
        String shortUrl,

        @Nullable
        String customUrl,

        @Nullable
        String deleteUrl,

        String userPreference
) {
}
