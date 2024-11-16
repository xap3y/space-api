package me.xap3y.space.service;

import me.xap3y.space.entity.User;
import me.xap3y.space.exception.InvalidApiKeyException;
import me.xap3y.space.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    private final UserRepository userRepository;

    public ApiKeyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User validateApiKey(String apiKey) throws InvalidApiKeyException {
        return userRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API Key"));
    }
}