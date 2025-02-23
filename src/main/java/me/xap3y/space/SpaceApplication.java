package me.xap3y.space;

import lombok.SneakyThrows;
import me.xap3y.space.api.enums.Environment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.time.LocalDateTime;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@ServletComponentScan
public class SpaceApplication {

    public static final String VERSION = "1.0-beta.12";
    public static LocalDateTime startedAt = null;
    public static final Environment env = Environment.DEVELOPMENT;

    @SneakyThrows
    public static void main(String[] args) {
        startedAt = LocalDateTime.now();
        SpringApplication.run(SpaceApplication.class, args);
    }

}
