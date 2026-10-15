package com.rks.infra;

import com.rks.infra.observabilidade.CorrelacaoFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrelacaoFilterTest {

    private final CorrelacaoFilter filtro = new CorrelacaoFilter();

    @Test
    void respostaTemIdDeCorrelacaoEMdcELimpo() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/readyz"), resposta, new MockFilterChain());

        assertNotNull(resposta.getHeader(CorrelacaoFilter.CABECALHO));
        assertFalse(resposta.getHeader(CorrelacaoFilter.CABECALHO).isBlank());
        assertNull(MDC.get(CorrelacaoFilter.CHAVE_MDC));
    }

    @Test
    void excecaoNaCadeiaEhRepassadaEMdcELimpo() {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        FilterChain quebrada = (req, res) -> {
            throw new IllegalStateException("falha simulada");
        };

        assertThrows(IllegalStateException.class,
                () -> filtro.doFilter(new MockHttpServletRequest("POST", "/api/admin/x"), resposta, quebrada));

        assertNull(MDC.get(CorrelacaoFilter.CHAVE_MDC));
    }
}
