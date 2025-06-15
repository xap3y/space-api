package me.xap3y.space.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.DiscordMeDto;
import me.xap3y.space.entity.DiscordConnection;
import me.xap3y.space.entity.User;
import me.xap3y.space.repository.DiscordConnectionRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class DiscordConnectionService {

    private final DiscordConnectionRepository discordConnectionRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ServerInfo serverInfo;

    public DiscordConnectionService(DiscordConnectionRepository discordConnectionRepository, ServerInfo serverInfo) {
        this.discordConnectionRepository = discordConnectionRepository;
        this.serverInfo = serverInfo;
    }

    public void deleteByDiscordId(String id) {
        discordConnectionRepository.deleteByDiscordId(id);
    }

    public void deleteByAccessToken(String id) {
        discordConnectionRepository.deleteByAccessToken(id);
    }

    public boolean existsByDiscordId(String id) {
        return discordConnectionRepository.existsByDiscordId(id);
    }

    public boolean existsByUserId(User user) {
        return discordConnectionRepository.existsByUserId(user);
    }

    public DiscordConnection save(DiscordConnection discordConnection) {
        return discordConnectionRepository.save(discordConnection);
    }

    public Optional<DiscordConnection> findByDiscordId(String discordId) {
        return discordConnectionRepository.findByDiscordId(discordId);
    }

    public Optional<DiscordConnection> findByAccessToken(String token) {
        return discordConnectionRepository.findByAccessToken(token);
    }

    public Optional<DiscordConnection> findById(Long id) {
        return discordConnectionRepository.findById(id);
    }

    public Optional<DiscordConnection> findByUserId(User user) {
        return discordConnectionRepository.findByUserId(user);
    }

    public DiscordMeDto fetchDiscordMe(String accessToken) throws JsonProcessingException {
        String url = "https://discord.com/api/users/@me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        DiscordMeDto user = mapper.readValue(response.getBody(), DiscordMeDto.class);

        return user;
    }

    public boolean revokeDiscordConnection(String accessToken) {
        try {
            String url = "https://discord.com/api/oauth2/token/revoke";

            String auth = serverInfo.getDiscordClientId() + ":" + serverInfo.getDiscordAuthBotToken();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            String body = "token=" + accessToken + "&token_type_hint=access_token";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            headers.set("Authorization", "Basic " + encodedAuth);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
