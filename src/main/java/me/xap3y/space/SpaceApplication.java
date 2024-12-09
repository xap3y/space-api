package me.xap3y.space;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@ServletComponentScan
public class SpaceApplication {

    /*public static final String VERSION = "{{ version }}";*/
    public static final String VERSION = "v0.2.0";

    public static void main(String[] args) {
        SpringApplication.run(SpaceApplication.class, args);
    }

}
