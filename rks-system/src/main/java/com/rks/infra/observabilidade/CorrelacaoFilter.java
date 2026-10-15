package com.rks.infra.observabilidade;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Dá um ID de correlação a cada requisição e registra respostas 5xx no log (AD-22).
 * Nunca registra corpo da requisição, cabeçalhos nem parâmetros: só método, caminho, status e ID.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacaoFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Correlation-Id";
    public static final String CHAVE_MDC = "correlacaoId";

    private static final Logger log = LoggerFactory.getLogger(CorrelacaoFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        MDC.put(CHAVE_MDC, id);
        response.setHeader(CABECALHO, id);
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException e) {
            log.error("Erro 5xx em {} {} (correlacaoId={})", request.getMethod(), request.getRequestURI(), id, e);
            throw e;
        } finally {
            if (response.getStatus() >= 500) {
                log.error("Resposta {} em {} {} (correlacaoId={})", response.getStatus(), request.getMethod(),
                        request.getRequestURI(), id);
            }
            MDC.remove(CHAVE_MDC);
        }
    }
}
