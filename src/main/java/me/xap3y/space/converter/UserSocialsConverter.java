package me.xap3y.space.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.xap3y.space.model.UserSocials;

@Converter
public class UserSocialsConverter implements AttributeConverter<UserSocials, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String convertToDatabaseColumn(UserSocials userSocials) {
        if (userSocials == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(userSocials);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting UserSocials to JSON", e);
        }
    }

    @Override
    public UserSocials convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, UserSocials.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to UserSocials", e);
        }
    }
}