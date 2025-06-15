package me.xap3y.space.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.xap3y.space.model.UserWebhookSettings;

@Converter(autoApply = true)
public class UserWebhookSettingsConverter implements AttributeConverter<UserWebhookSettings, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String convertToDatabaseColumn(UserWebhookSettings webhookSettings) {
        if (webhookSettings == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(webhookSettings);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting UserWebhookSettings to JSON", e);
        }
    }

    @Override
    public UserWebhookSettings convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        try {
            return objectMapper.readValue(s, UserWebhookSettings.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to UserWebhookSettings", e);
        }
    }
}
