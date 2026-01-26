package me.xap3y.space.mapper;

import lombok.AllArgsConstructor;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.UserSettings;
import me.xap3y.space.service.UserSettingsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@AllArgsConstructor
public class ImageMapper implements Function<Image, ImageInfoDto> {

    private final UserSettingsService userSettingsService;
    private final UrlSetMapper urlSetMapper;
    private final ShortUserMapper shortUserMapper;

    // UserId == requester (for url preferences)
    public ImageInfoDto apply(Image image, @NotNull Long userId) {
        return new ImageInfoDto(
                image.getUniqueId(),
                image.getFileType(),
                image.getDescription(),
                image.getSize(),
                image.getUploadTime(),
                image.getExpirationTime(),
                urlSetMapper.apply(image, userId),
                shortUserMapper.apply(image.getUploader()),
                image.getPassword() != null,
                image.isPublic(),
                image.getLocation(),
                userSettingsService.getUserSettingsByUserId(image.getUploader().getId()).map(UserSettings::getEmbedSettings).orElse(null)
        );
    }

    @Override
    public ImageInfoDto apply(Image image) {
        return apply(image, image.getUploader().getId());
    }
}
