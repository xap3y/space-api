package me.xap3y.space.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagedResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {
}
