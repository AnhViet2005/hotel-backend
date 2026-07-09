package com.hotel.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource(Environment env) throws Exception {
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return null;
        }

        URI dbUri = new URI(databaseUrl);
        String username = null;
        String password = null;
        String userInfo = dbUri.getUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":", 2);
            username = parts[0];
            if (parts.length > 1) password = parts[1];
        }

        String host = dbUri.getHost();
        int port = dbUri.getPort();
        String path = dbUri.getPath();
        String query = dbUri.getQuery();

        StringBuilder jdbc = new StringBuilder();
        jdbc.append("jdbc:postgresql://").append(host).append(":").append(port).append(path);
        if (query != null && !query.isBlank()) {
            // preserve existing query params and ensure sslmode=require
            jdbc.append("?").append(query).append("&sslmode=require");
        } else {
            jdbc.append("?sslmode=require");
        }

        return DataSourceBuilder.create()
                .url(jdbc.toString())
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
