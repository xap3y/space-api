package me.xap3y.space.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.xap3y.space.api.enums.StatTimePreset;
import me.xap3y.space.util.CustomLocalDateTimeDeserializer;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TotalStatsRequest {

    @Nullable
    private StatTimePreset preset;

    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @Nullable
    private LocalDateTime fromDate;

    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @Nullable
    private LocalDateTime toDate;
}
