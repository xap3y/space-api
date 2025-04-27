package me.xap3y.space.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.xap3y.space.util.CustomLocalDateTimeDeserializer;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatsRequest {

    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @Nullable
    private LocalDateTime fromDate;

    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @Nullable
    private LocalDateTime toDate;

    @Nullable
    private Integer limit;

    @Nullable
    private Boolean fillMissing;

    public StatsRequest() {}

}
