/*
package me.xap3y.space.mapper;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.UserSettings;
import me.xap3y.space.model.UserInviter;
import me.xap3y.space.model.UserWebhookSettings;
import me.xap3y.space.service.UserSettingsService;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;

@Service
public class ImageInfoMapper implements Function<Pair<String, ImageDto>, ImageInfoDto> {

    private final ServerInfo serverInfo;
    private final UserInvitorMapper userInvitorMapper;
    private final UserSettingsService userSettingsService;
    private final ShortUserMapper shortUserMapper;

    public ImageInfoMapper(ServerInfo serverInfo, UserInvitorMapper userInvitorMapper, UserSettingsService userSettingsService, ShortUserMapper shortUserMapper) {
        this.serverInfo = serverInfo;
        this.userInvitorMapper = userInvitorMapper;
        this.userSettingsService = userSettingsService;
        this.shortUserMapper = shortUserMapper;
    }

    @Override
    public ImageInfoDto apply(Pair<String, ImageDto> imageDto) {

        String imageUrl = switch (imageDto.getSecond().location()) {
            case R2 -> "https://r2.xap3y.eu/media/" + imageDto.getFirst();
            case LOCAL -> serverInfo.getBaseUrl() + "/v1/image/get/" + imageDto.getFirst();
            default -> serverInfo.getBaseUrl() + "/web/image-render/" + imageDto.getFirst();
        };

        UrlSetDto urlSet = new UrlSetDto(
                serverInfo.getBaseUrl() + "/web/image-render/" + imageDto.getFirst(),
                serverInfo.getFrontEndUrl() + "/i/" + imageDto.getFirst(),
                imageUrl,
                serverInfo.getShortImageUrl() + "/" + imageDto.getFirst(),
                null,
                null,
                null
        );

        ImageDto dto = imageDto.getSecond();

        UserWebhookSettings webhookSettings = null;

        Optional<UserSettings> userSettings = userSettingsService.getUserSettingsByUserId(dto.uploader().getId());
        if (userSettings.isPresent()) {
            webhookSettings = userSettings.get().getEmbedSettings();
        }

        return new ImageInfoDto(
                imageDto.getFirst(),
                dto.type(),
                dto.description(),
                dto.size(),
                dto.uploadedAt(),
                dto.expiresAt(),
                urlSet,
                shortUserMapper.apply(dto.uploader()),
                dto.password() != null,
                dto.isPublic(),
                dto.location(),
                webhookSettings
        );
    }
}
*/
