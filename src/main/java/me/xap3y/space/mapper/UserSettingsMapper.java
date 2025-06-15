package me.xap3y.space.mapper;

import me.xap3y.space.dto.UserSettingsDto;
import me.xap3y.space.entity.UserSettings;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UserSettingsMapper implements Function<UserSettings, UserSettingsDto> {

    @Override
    public UserSettingsDto apply(UserSettings userSettings) {
        return new UserSettingsDto(
                userSettings.getUserId().getId(),
                userSettings.getUserId().getUsername(),
                userSettings.getEmbedSettings()
        );
    }
}
