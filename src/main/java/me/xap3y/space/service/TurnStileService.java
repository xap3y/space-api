package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.api.exception.ResourceAccessForbiddenException;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.controller.DiscordTranscriptController;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class TurnStileService {

    private final ServerInfo serverInfo;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean validate(String token) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", serverInfo.getTurnstileSecret());
        form.add("response", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        ResponseEntity<DiscordTranscriptController.TurnstileSiteverifyResponse> resp = restTemplate.exchange(
                "https://challenges.cloudflare.com/turnstile/v0/siteverify",
                HttpMethod.POST,
                requestEntity,
                DiscordTranscriptController.TurnstileSiteverifyResponse.class
        );

        DiscordTranscriptController.TurnstileSiteverifyResponse data = resp.getBody();

        if (data == null || !Boolean.TRUE.equals(data.success)) {
            throw new ResourceAccessForbiddenException("Invalid token");
        }

        return true;
    }
}
