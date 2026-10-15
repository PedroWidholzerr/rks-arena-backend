package com.rks.infra;

import com.rks.infra.observabilidade.CabecalhosDeCacheFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CabecalhosDeCacheFilterTest {

    private final CabecalhosDeCacheFilter filtro = new CabecalhosDeCacheFilter();

    private String cacheDe(String metodo, String caminho) throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        filtro.doFilter(new MockHttpServletRequest(metodo, caminho), resposta, new MockFilterChain());
        return resposta.getHeader("Cache-Control");
    }

    @Test
    void endpointsPublicosDeDadosPodemFicarNaBorda() throws Exception {
        assertEquals("public, s-maxage=30, stale-while-revalidate=86400", cacheDe("GET", "/api/public/seasons"));
        assertEquals("public, s-maxage=30, stale-while-revalidate=86400", cacheDe("GET", "/api/public/phases/x/standings"));
    }

    @Test
    void oStatusNuncaVemDeCache() throws Exception {
        assertEquals("no-store", cacheDe("GET", "/api/public/status"));
    }

    @Test
    void adminELoginNuncaFicamEmCacheDeBorda() throws Exception {
        assertEquals("no-store", cacheDe("GET", "/api/admin/users"));
        assertEquals("no-store", cacheDe("POST", "/api/auth/login"));
    }

    @Test
    void outrasRotasNaoRecebemCabecalho() throws Exception {
        assertNull(cacheDe("GET", "/readyz"));
    }
}
