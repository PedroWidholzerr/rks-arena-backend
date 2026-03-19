package com.rks.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataSourceConfigTest {

    private final DataSourceConfig dataSourceConfig = new DataSourceConfig();

    @Test
    void shouldPreferDatabaseUrlOverApplicationPropertyFallback() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/rks_arena")
                .withProperty("spring.datasource.username", "postgres")
                .withProperty("spring.datasource.password", "1234")
                .withProperty("DATABASE_URL", "postgresql://railway_user:railway_pass@containers-us-west-123.railway.app:6543/railway_db");

        HikariDataSource dataSource = unwrap(dataSourceConfig.dataSource(environment));

        assertEquals("jdbc:postgresql://containers-us-west-123.railway.app:6543/railway_db", dataSource.getJdbcUrl());
        assertEquals("railway_user", dataSource.getUsername());
        assertEquals("railway_pass", dataSource.getPassword());
    }

    @Test
    void shouldBuildJdbcUrlFromPgEnvironmentAliases() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("POSTGRES_HOST", "db.internal")
                .withProperty("POSTGRES_PORT", "5432")
                .withProperty("POSTGRES_DB", "arena")
                .withProperty("POSTGRES_USER", "arena_user")
                .withProperty("POSTGRES_PASSWORD", "arena_pass");

        HikariDataSource dataSource = unwrap(dataSourceConfig.dataSource(environment));

        assertEquals("jdbc:postgresql://db.internal:5432/arena", dataSource.getJdbcUrl());
        assertEquals("arena_user", dataSource.getUsername());
        assertEquals("arena_pass", dataSource.getPassword());
    }

    private HikariDataSource unwrap(DataSource dataSource) {
        return (HikariDataSource) dataSource;
    }
}