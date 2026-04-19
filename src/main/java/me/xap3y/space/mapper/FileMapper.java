package me.xap3y.space.mapper;

import me.xap3y.space.dto.FileInfoDto;
import me.xap3y.space.entity.FileEntity;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class FileMapper implements Function<FileEntity, FileInfoDto> {

    @Override
    public FileInfoDto apply(FileEntity file) {
        return new FileInfoDto(
                file.getUniqueId(),
                file.getFileName(),
                file.getFileType(),
                file.getSize(),
                file.getDescription(),
                file.getUploadTime(),
                file.getExpirationTime(),
                file.getLocation(),
                file.getSource()
        );
    }
}