package me.xap3y.space.service;

import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.entity.ApiKey;
import me.xap3y.space.entity.User;
import me.xap3y.space.repository.ApiKeyRepository;
import me.xap3y.space.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User validateApiKey(String apiKey) throws InvalidApiKeyException {
        ApiKey apiKeyEntity = apiKeyRepository.findByKeyCode(apiKey)
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API Key!"));
        return userRepository.findByApiKey(apiKeyEntity)
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API Key!"));
    }
}