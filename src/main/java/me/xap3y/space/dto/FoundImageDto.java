package me.xap3y.space.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@AllArgsConstructor
@Data
public class FoundImageDto {
    private String originalName;
    private String mimeType;
    private int size;
    private Optional<ImageInfoDto> uploadedImageInfo = Optional.empty();

    public FoundImageDto(String name, String fileType, int size) {
        this.originalName = name;
        this.mimeType = fileType;
        this.size = size;
    }
}
