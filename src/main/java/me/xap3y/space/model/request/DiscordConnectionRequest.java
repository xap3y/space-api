package me.xap3y.space.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscordConnectionRequest {

    private String discordId;

    private String accessToken;

    private String refreshToken;

    private Long expiresAt;
}
