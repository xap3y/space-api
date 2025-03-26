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

    public ImageMapper(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public ImageInfoDto apply(Image image) {
        return new ImageInfoDto(
                image.getUniqueId(),
                image.getFileType(),
                image.getSize(),
                image.getUploadTime(),
                new UrlSetDto(
                        serverInfo.getBaseUrl() + "/web/image-render/" + image.getUniqueId(),
                        serverInfo.getFrontEndUrl() + "/image/" + image.getUniqueId(),
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
                        image.getUploader().getInvitedBy() != null ? new UserInviter(
                                image.getUploader().getInvitedBy().getId(),
                                image.getUploader().getInvitedBy().getUsername(),
                                image.getUploader().getInvitedBy().getRole()
                        ) : null
                )
        );
    }
}
