package me.xap3y.space.mapper;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.LowUserDto;
import me.xap3y.space.dto.PlaylistImageDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.Image;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class PlaylistImageMapper implements Function<Image, PlaylistImageDto> {

    private final ServerInfo serverInfo;

    public PlaylistImageMapper(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public PlaylistImageDto apply(Image image) {
        return new PlaylistImageDto(
                image.getUniqueId(),
                image.getFileType(),
                image.getDescription(),
                image.getSize(),
                image.getUploadTime(),
                new UrlSetDto(
                        serverInfo.getBaseUrl() + "/web/image-render/" + image.getUniqueId(),
                        serverInfo.getFrontEndUrl() + "/i/" + image.getUniqueId(),
                        serverInfo.getBaseUrl() + "/v1/image/get/" + image.getUniqueId(),
                        serverInfo.getShortImageUrl() + "/" + image.getUniqueId(),
                        null
                ),
                new LowUserDto(image.getUploader().getId(), image.getUploader().getUsername())
        );
    }
}
