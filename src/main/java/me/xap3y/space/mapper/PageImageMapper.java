package me.xap3y.space.mapper;

import me.xap3y.space.dto.PageImage;
import me.xap3y.space.entity.Image;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class PageImageMapper implements Function<Image, PageImage> {

    private final UrlSetMapper urlSetMapper;

    public PageImageMapper(UrlSetMapper urlSetMapper) {
        this.urlSetMapper = urlSetMapper;
    }

    @Override
    public PageImage apply(Image image) {
        return PageImage.builder()
                .uniqueId(image.getUniqueId())
                .type(image.getFileType())
                .description(image.getDescription())
                .size(image.getSize())
                .uploadedAt(image.getUploadTime())
                .expiresAt(image.getExpirationTime())
                .requiresPassword(image.getPassword() != null)
                .isPublic(image.isPublic())
                .hasPoster(image.isPoster())
                .location(image.getLocation())
                .urls(urlSetMapper.apply(image))
                .build();
    }
}
