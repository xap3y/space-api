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
    private final UserInvitorMapper userInvitorMapper;

    public ImageInfoMapper(ServerInfo serverInfo, UserInvitorMapper userInvitorMapper) {
        this.serverInfo = serverInfo;
        this.userInvitorMapper = userInvitorMapper;
    }

    @Override
    public ImageInfoDto apply(Pair<String, ImageDto> imageDto) {

        String imageUrl = switch (imageDto.getSecond().location()) {
            case R2 -> "https://r3.xap3y.space/media/" + imageDto.getFirst();
            case LOCAL -> serverInfo.getBaseUrl() + "/v1/image/get/" + imageDto.getFirst();
            default -> serverInfo.getBaseUrl() + "/web/image-render/" + imageDto.getFirst();
        };

        UrlSetDto urlSet = new UrlSetDto(
                serverInfo.getBaseUrl() + "/web/image-render/" + imageDto.getFirst(),
                serverInfo.getFrontEndUrl() + "/i/" + imageDto.getFirst(),
                imageUrl,
                serverInfo.getShortImageUrl() + "/" + imageDto.getFirst(),
                null
        );

        ImageDto dto = imageDto.getSecond();
        return new ImageInfoDto(
                imageDto.getFirst(),
                dto.type(),
                dto.description(),
                dto.size(),
                dto.uploadedAt(),
                dto.expiresAt(),
                urlSet,
                (dto.uploader() != null) ? new ShortUserDto(
                        dto.uploader().getId(),
                        dto.uploader().getUsername(),
                        dto.uploader().getRole(),
                        dto.uploader().getAvatar(),
                        dto.uploader().getCreatedAt(),
                        dto.uploader().getInvitedBy() != null ? userInvitorMapper.apply(dto.uploader().getInvitedBy()) : null
                ): null,
                dto.password() != null,
                dto.isPublic(),
                dto.location()
        );
    }
}
