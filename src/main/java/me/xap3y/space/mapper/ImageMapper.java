package me.xap3y.space.mapper;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.model.UserInviter;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class ImageMapper implements Function<Image, ImageInfoDto> {


    private final ServerInfo serverInfo;
    private final UserInvitorMapper userInvitorMapper;

    public ImageMapper(ServerInfo serverInfo, UserInvitorMapper userInvitorMapper) {
        this.serverInfo = serverInfo;
        this.userInvitorMapper = userInvitorMapper;
    }

    @Override
    public ImageInfoDto apply(Image image) {
        return new ImageInfoDto(
                image.getUniqueId(),
                image.getFileType(),
                image.getDescription(),
                image.getSize(),
                image.getUploadTime(),
                image.getExpirationTime(),
                new UrlSetDto(
                        serverInfo.getBaseUrl() + "/web/image-render/" + image.getUniqueId(),
                        serverInfo.getFrontEndUrl() + "/i/" + image.getUniqueId(),
                        serverInfo.getBaseUrl() + "/v1/image/get/" + image.getUniqueId(),
                        serverInfo.getShortImageUrl() + "/" + image.getUniqueId(),
                        null
                ),
                new ShortUserDto(
                        image.getUploader().getId(),
                        image.getUploader().getUsername(),
                        image.getUploader().getRole(),
                        image.getUploader().getAvatar(),
                        image.getUploader().getCreatedAt(),
                        image.getUploader().getInvitedBy() != null ? userInvitorMapper.apply(image.getUploader().getInvitedBy()) : null
                ),
                image.getPassword() != null,
                image.getIsPublic()
        );
    }
}
