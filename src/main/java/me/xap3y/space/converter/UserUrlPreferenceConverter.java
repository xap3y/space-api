package me.xap3y.space.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.xap3y.space.model.UserUrlPreferenceSettings;

@Converter(autoApply = true)
public class UserUrlPreferenceConverter implements AttributeConverter<UserUrlPreferenceSettings, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String convertToDatabaseColumn(UserUrlPreferenceSettings webhookSettings) {
        if (webhookSettings == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(webhookSettings);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting UserUrlPreferenceSettings to JSON", e);
        }
    }

    @Override
    public UserUrlPreferenceSettings convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        try {
            return objectMapper.readValue(s, UserUrlPreferenceSettings.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to UserUrlPreferenceSettings", e);
        }
    }
}
