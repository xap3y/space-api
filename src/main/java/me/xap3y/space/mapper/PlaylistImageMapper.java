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
    private final UrlSetMapper urlSetMapper;

    public PlaylistImageMapper(ServerInfo serverInfo, UrlSetMapper urlSetMapper) {
        this.serverInfo = serverInfo;
        this.urlSetMapper = urlSetMapper;
    }

    @Override
    public PlaylistImageDto apply(Image image) {
        return new PlaylistImageDto(
                image.getUniqueId(),
                image.getFileType(),
                image.getDescription(),
                image.getSize(),
                image.getUploadTime(),
                urlSetMapper.apply(image),
                new LowUserDto(image.getUploader().getId(), image.getUploader().getUsername())
        );
    }
}
