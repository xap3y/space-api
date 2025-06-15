package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.entity.UserSettings;
import me.xap3y.space.repository.UserSettingsRepository;
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public UserSettings saveUserSettings(UserSettings userSettings) {
        return userSettingsRepository.save(userSettings);
    }

    public Optional<UserSettings> getUserSettingsByUserId(Long userId) {
        return userSettingsRepository.findByUserId(userId);
    }

    public boolean existByUserId(Long userId) {
        return userSettingsRepository.existsByUserId(userId);
    }
}
