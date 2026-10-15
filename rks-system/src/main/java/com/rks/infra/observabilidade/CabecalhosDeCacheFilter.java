package com.rks.infra.observabilidade;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Respostas do admin e de login nunca ficam em cache de borda (UX-DR14, C-2). As públicas de dados levam {@code s-maxage=30} com {@code stale-while-revalidate} (história 2.33); o front
 * mostra "atualizado às HH:mm" pelo {@code atualizadoEm}, então o dado defasado nunca se passa por novo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CabecalhosDeCacheFilter extends OncePerRequestFilter {

    /** A borda serve a última versão por 30 s e, com o backend dormindo, até 1 dia enquanto revalida (AD-20). */
    public static final String CACHE_PUBLICO = "public, s-maxage=30, stale-while-revalidate=86400";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String caminho = request.getRequestURI();
        if (caminho.startsWith("/api/admin/") || caminho.startsWith("/api/auth/")) {
            response.setHeader("Cache-Control", "no-store");
        } else if (caminho.equals("/api/public/status")) {
            // O front usa o status para saber se o backend acordou: nunca vem de cache.
            response.setHeader("Cache-Control", "no-store");
        } else if (caminho.startsWith("/api/public/") && "GET".equals(request.getMethod())) {
            response.setHeader("Cache-Control", CACHE_PUBLICO);
        }
        chain.doFilter(request, response);
    }
}
