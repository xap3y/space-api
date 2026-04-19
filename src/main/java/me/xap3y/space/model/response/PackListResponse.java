package me.xap3y.space.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import me.xap3y.space.dto.FileInfoDto;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PackListResponse(
        // For single pack response
        String packId,
        String description,
        Boolean isComplete,
        Integer totalFiles,
        List<FileInfoDto> files,
        Long totalSize,
        LocalDateTime uploadTime,
        Boolean isPasswordProtected,

        // For list response
        List<?> packs,
        Long totalElements,
        Integer totalPages,
        Integer currentPage,
        Integer pageSize
) {
    // Constructor for single pack
    public PackListResponse(String packId, String description, Boolean isComplete,
                            Integer totalFiles, Long totalSize, LocalDateTime uploadTime,
                            Boolean isPasswordProtected) {
        this(packId, description, isComplete, totalFiles, null, totalSize, uploadTime,
                isPasswordProtected, null, null, null, null, null);
    }

    // Constructor for list of packs
    public PackListResponse(List<?> packs, Long totalElements, Integer totalPages,
                            Integer currentPage, Integer pageSize) {
        this(null, null, null, null, null, null, null, null, packs, totalElements,
                totalPages, currentPage, pageSize);
    }
}