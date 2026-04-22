package me.xap3y.space.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import me.xap3y.space.api.enums.ImageLocation;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageImage {
    private String uniqueId;
    private String type;
    private String description;
    private long size;
    private LocalDateTime uploadedAt;
    private LocalDateTime expiresAt;
    private boolean requiresPassword;
    private boolean isPublic;
    private boolean hasPoster;
    private ImageLocation location;
    private UrlSetDto urls;
}
