package com.rks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataSourceConfigTest {

    @Test
    void configurationLoads() {
        DataSourceConfig config = new DataSourceConfig();
        assertNotNull(config);
    }
}