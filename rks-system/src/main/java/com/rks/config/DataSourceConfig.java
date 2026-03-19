package com.rks.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {
        String rawUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("JDBC_DATABASE_URL"),
                environment.getProperty("DATABASE_PRIVATE_URL"),
                environment.getProperty("DATABASE_PUBLIC_URL"),
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("POSTGRES_URL"),
                environment.getProperty("spring.datasource.url")
        );

        ParsedDatabaseUrl parsedDatabaseUrl = parseDatabaseUrl(rawUrl);

        String jdbcUrl = StringUtils.hasText(parsedDatabaseUrl.jdbcUrl())
                ? parsedDatabaseUrl.jdbcUrl()
                : buildJdbcUrlFromPgEnvironment(environment);

        String username = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_USERNAME"),
                environment.getProperty("PGUSER"),
                environment.getProperty("POSTGRES_USER"),
                environment.getProperty("DB_USER"),
                parsedDatabaseUrl.username(),
                environment.getProperty("spring.datasource.username")
        );

        String password = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
                environment.getProperty("PGPASSWORD"),
                environment.getProperty("POSTGRES_PASSWORD"),
                environment.getProperty("DB_PASSWORD"),
                parsedDatabaseUrl.password(),
                environment.getProperty("spring.datasource.password")
        );

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }

    private String buildJdbcUrlFromPgEnvironment(Environment environment) {
        String host = firstNonBlank(
            environment.getProperty("PGHOST"),
            environment.getProperty("POSTGRES_HOST"),
            environment.getProperty("DB_HOST"),
            "localhost"
        );
        String port = firstNonBlank(
            environment.getProperty("PGPORT"),
            environment.getProperty("POSTGRES_PORT"),
            environment.getProperty("DB_PORT"),
            "5432"
        );
        String database = firstNonBlank(
            environment.getProperty("PGDATABASE"),
            environment.getProperty("POSTGRES_DB"),
            environment.getProperty("DB_NAME"),
            "rks_arena"
        );
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private ParsedDatabaseUrl parseDatabaseUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return new ParsedDatabaseUrl(null, null, null);
        }

        if (rawUrl.startsWith("jdbc:")) {
            return new ParsedDatabaseUrl(rawUrl, null, null);
        }

        if (!rawUrl.startsWith("postgres://") && !rawUrl.startsWith("postgresql://")) {
            return new ParsedDatabaseUrl(rawUrl, null, null);
        }

        URI uri = URI.create(rawUrl);
        String userInfo = uri.getUserInfo();
        String username = null;
        String password = null;

        if (StringUtils.hasText(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost());

        if (uri.getPort() > 0) {
            jdbcUrl.append(":").append(uri.getPort());
        }

        jdbcUrl.append(uri.getPath());

        if (StringUtils.hasText(uri.getQuery())) {
            jdbcUrl.append("?").append(uri.getQuery());
        }

        return new ParsedDatabaseUrl(jdbcUrl.toString(), username, password);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}