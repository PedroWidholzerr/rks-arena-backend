package com.rks.infra.persistencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Testa se o banco responde, para o /readyz. */
@Component
public class VerificadorBanco {

    private static final Logger log = LoggerFactory.getLogger(VerificadorBanco.class);

    private final JdbcTemplate jdbc;

    public VerificadorBanco(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean disponivel() {
        try {
            return Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class));
        } catch (RuntimeException e) {
            log.warn("Banco indisponível no /readyz: {}", e.getClass().getName());
            return false;
        }
    }
}
