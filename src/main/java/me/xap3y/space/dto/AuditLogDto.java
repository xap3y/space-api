package me.xap3y.space.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.xap3y.space.api.enums.PortalLogType;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogDto {

    public Long id;
    public ShortUserDto user;
    public PortalLogType type;

    @Nullable
    public String description;

    @Nullable
    public String source;

    public LocalDateTime time;
}
