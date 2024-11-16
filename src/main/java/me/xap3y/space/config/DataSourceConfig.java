package me.xap3y.space.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;

public class DataSourceConfig {

    private DatabaseConfigProperties configProperties;

    public DataSourceConfig(DatabaseConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    public DataSource dataSource() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        configProperties = mapper.readValue(
                new ClassPathResource("config.json").getFile(),
                DatabaseConfigProperties.class
        );

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(configProperties.getDatabase().getUrl());
        dataSource.setUsername(configProperties.getDatabase().getUsername());
        dataSource.setPassword(configProperties.getDatabase().getPassword());
        dataSource.setDriverClassName(configProperties.getDatabase().getDriverClassName());

        return dataSource;
    }
}
