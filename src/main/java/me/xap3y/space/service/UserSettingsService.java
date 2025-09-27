package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.entity.User;
import me.xap3y.space.entity.UserSettings;
import me.xap3y.space.model.UserUrlPreferenceSettings;
import me.xap3y.space.model.UserWebhookSettings;
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

    public UserSettings getUserSettingsByUserIdStrict(Long userId) {
        return userSettingsRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("User settings not found"));
    }

    public boolean existByUserId(Long userId) {
        return userSettingsRepository.existsByUserId(userId);
    }

    public Optional<UserSettings> createDefaultSettingsForUser(User uploader) {
        if (existByUserId(uploader.getId())) return Optional.empty();

        UserSettings newSettings = new UserSettings();
        newSettings.setUserId(uploader);
        newSettings.setEmbedSettings(new UserWebhookSettings());
        newSettings.setUrlSettings(new UserUrlPreferenceSettings());
        return Optional.of(saveUserSettings(newSettings));
    }
}
