package me.xap3y.space.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import me.xap3y.space.dto.PageImage;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImageListResponse(
        List<PageImage> images,

        Long totalElements,
        Integer totalPages,
        Integer currentPage,
        Integer pageSize
) {
}
