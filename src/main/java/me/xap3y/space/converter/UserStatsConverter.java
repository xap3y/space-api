package me.xap3y.space.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.xap3y.space.model.UserStats;

@Converter
public class UserStatsConverter implements AttributeConverter<UserStats, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String convertToDatabaseColumn(UserStats userStatsDto) {
        if (userStatsDto == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(userStatsDto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting UserStats to JSON", e);
        }
    }

    @Override
    public UserStats convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        try {
            return objectMapper.readValue(s, UserStats.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to UserStats", e);
        }
    }
}
