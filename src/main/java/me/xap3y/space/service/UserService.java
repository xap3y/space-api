package me.xap3y.space.service;

import me.xap3y.space.entity.User;
import me.xap3y.space.exception.ResourceNotFoundException;
import me.xap3y.space.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String username, String password, String role) {

        String apiKey = generateApiKey();

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User(username, encodedPassword, role, apiKey);
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User findByApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateApiKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder apiKey = new StringBuilder(8);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < 8; i++) {
            apiKey.append(characters.charAt(random.nextInt(characters.length())));
        }

        return apiKey.toString();
    }
}
