package me.xap3y.space.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String dateTime = parser.getText();

        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (dateTime.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return LocalDate.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            .atStartOfDay();
                }

                return LocalDateTime.parse(dateTime, formatter);
            } catch (DateTimeParseException ignored) {
                // IGNORE
            }
        }

        throw new IOException("Invalid date format: " + dateTime);
    }
}