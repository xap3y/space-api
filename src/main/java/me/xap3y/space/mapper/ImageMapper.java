package me.xap3y.space.mapper;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.UserSettings;
import me.xap3y.space.model.UserInviter;
import me.xap3y.space.service.UserSettingsService;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class ImageMapper implements Function<Image, ImageInfoDto> {


    private final ServerInfo serverInfo;
    private final UserInvitorMapper userInvitorMapper;
    private final UserSettingsService userSettingsService;
    private final UrlSetMapper urlSetMapper;
    private final ShortUserMapper shortUserMapper;

    public ImageMapper(ServerInfo serverInfo, UserInvitorMapper userInvitorMapper, UserSettingsService userSettingsService, UrlSetMapper urlSetMapper, ShortUserMapper shortUserMapper) {
        this.serverInfo = serverInfo;
        this.userInvitorMapper = userInvitorMapper;
        this.userSettingsService = userSettingsService;
        this.urlSetMapper = urlSetMapper;
        this.shortUserMapper = shortUserMapper;
    }

    @Override
    public ImageInfoDto apply(Image image) {

        /*String imageUrl = switch (image.getLocation()) {
            case R2 -> "https://r3.xap3y.space/media/" + image.getUniqueId();
            case LOCAL -> serverInfo.getBaseUrl() + "/v1/image/get/" + image.getUniqueId();
            default -> serverInfo.getBaseUrl() + "/web/image-render/" + image.getUniqueId();
        };*/

        return new ImageInfoDto(
                image.getUniqueId(),
                image.getFileType(),
                image.getDescription(),
                image.getSize(),
                image.getUploadTime(),
                image.getExpirationTime(),
                urlSetMapper.apply(image),
                shortUserMapper.apply(image.getUploader()),
                image.getPassword() != null,
                image.isPublic(),
                image.getLocation(),
                userSettingsService.getUserSettingsByUserId(image.getUploader().getId()).map(UserSettings::getEmbedSettings).orElse(null)
        );
    }
}
