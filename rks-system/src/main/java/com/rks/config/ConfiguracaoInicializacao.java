package com.rks.config;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoInicializacao {

    /** Com lazy-initialization, o Flyway precisa rodar na subida: a migração não pode esperar o primeiro uso. */
    @Bean
    static LazyInitializationExcludeFilter excluiFlywayDaInicializacaoPreguicosa() {
        return (nomeDoBean, definicao, tipo) -> tipo.getName().toLowerCase().contains("flyway");
    }
}
