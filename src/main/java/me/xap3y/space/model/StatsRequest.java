package me.xap3y.space.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.xap3y.space.util.CustomLocalDateTimeDeserializer;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StatsRequest {

    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime fromDate;

    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime toDate;

    public StatsRequest() {}

}
