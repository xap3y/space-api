package me.xap3y.space.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties
@Getter
public class DatabaseConfigProperties {

    @JsonProperty("database")
    private DatabaseProperties database = new DatabaseProperties();

    @Setter
    @Getter
    public static class DatabaseProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }

}
