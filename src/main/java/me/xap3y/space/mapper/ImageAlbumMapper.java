package me.xap3y.space.mapper;

import me.xap3y.space.dto.ImageAlbumDto;
import me.xap3y.space.dto.PlaylistImageDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.ImagePlaylist;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class ImageAlbumMapper implements Function<ImagePlaylist, ImageAlbumDto> {

    private final ShortUserMapper shortUserMapper;
    private final PlaylistImageMapper playlistImageMapper;

    public ImageAlbumMapper(ShortUserMapper shortUserMapper, PlaylistImageMapper playlistImageMapper) {
        this.shortUserMapper = shortUserMapper;
        this.playlistImageMapper = playlistImageMapper;
    }

    @Override
    public ImageAlbumDto apply(ImagePlaylist imagePlaylist) {
        return new ImageAlbumDto(
                imagePlaylist.getId(),
                imagePlaylist.getUniqueId(),
                imagePlaylist.getDescription(),
                imagePlaylist.getCreatedAt(),
                shortUserMapper.apply(imagePlaylist.getUploader()),
                imagePlaylist.getImages().stream()
                        .map(playlistImageMapper)
                        .toList()
        );
    }
}
