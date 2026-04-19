package me.xap3y.space.model.response;

import me.xap3y.space.dto.FileInfoDto;

import java.time.LocalDateTime;
import java.util.List;

public record FileUploadResponse(
        boolean error,
        String packId,
        List<FileInfoDto> files,
        int totalFiles,
        long totalSize,
        LocalDateTime uploadTime
) {}