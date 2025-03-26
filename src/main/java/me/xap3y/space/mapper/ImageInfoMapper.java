package me.xap3y.space.mapper;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.model.UserInviter;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class ImageInfoMapper implements Function<Pair<String, ImageDto>, ImageInfoDto> {

    private final ServerInfo serverInfo;

    public ImageInfoMapper(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public ImageInfoDto apply(Pair<String, ImageDto> imageDto) {
        ImageDto dto = imageDto.getSecond();
        return new ImageInfoDto(
                imageDto.getFirst(),
                dto.type(),
                dto.size(),
                dto.uploadedAt(),
                new UrlSetDto(
                        serverInfo.getBaseUrl() + "/web/image-render/" + imageDto.getFirst(),
                        serverInfo.getFrontEndUrl() + "/image/" + imageDto.getFirst(),
                        serverInfo.getBaseUrl() + "/v1/image/get/" + imageDto.getFirst(),
                    serverInfo.getShortImageUrl() + "/" + imageDto.getFirst(),
                        null
                ),
                new ShortUserDto(
                        dto.uploader().getId(),
                        dto.uploader().getUsername(),
                        dto.uploader().getRole(),
                        dto.uploader().getAvatar(),
                        dto.uploader().getCreatedAt(),
                        dto.uploader().getInvitedBy() != null ? new UserInviter(
                                dto.uploader().getInvitedBy().getId(),
                                dto.uploader().getInvitedBy().getUsername(),
                                dto.uploader().getInvitedBy().getRole()
                        ) : null
                )
        );
    }
}
