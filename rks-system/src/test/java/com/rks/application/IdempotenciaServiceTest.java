package com.rks.application;

import com.rks.application.partidas.IdempotenciaService;
import com.rks.infra.persistencia.ChaveIdempotenciaRepositorio;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class IdempotenciaServiceTest {

    private final IdempotenciaService servico =
            new IdempotenciaService(mock(ChaveIdempotenciaRepositorio.class), JsonMapper.builder().build());

    @Test
    void escopoCabeNaColunaDe80Caracteres() {
        String escopo = servico.escopo("PUT /api/admin/matches/" + UUID.randomUUID() + "/result",
                Map.of("version", 3, "jogosA", 2, "jogosB", 1));

        assertTrue(escopo.length() <= 80, "escopo com " + escopo.length() + " caracteres");
    }

    @Test
    void mesmoPedidoDaOMesmoEscopoEPedidoDiferenteNao() {
        String rota = "PUT /api/admin/matches/x/result";

        assertEquals(servico.escopo(rota, Map.of("a", 1)), servico.escopo(rota, Map.of("a", 1)));
        assertNotEquals(servico.escopo(rota, Map.of("a", 1)), servico.escopo(rota, Map.of("a", 2)));
        assertNotEquals(servico.escopo(rota, Map.of("a", 1)), servico.escopo("PUT /outra", Map.of("a", 1)));
    }
}
