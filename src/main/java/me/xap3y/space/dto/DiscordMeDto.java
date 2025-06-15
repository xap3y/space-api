package me.xap3y.space.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordMeDto(
        String id,
        String username,
        String avatar,
        String discriminator,

        @JsonProperty("global_name")
        String globalName,

        @JsonProperty("banner_color\t")
        String bannerColor,
        String locale,
        String email,
        String verified
) { }
