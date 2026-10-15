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
 * Respostas do admin e de login nunca ficam em cache de borda (UX-DR14, C-2). As públicas ficam sem
 * cache até a história 2.33 medir o {@code s-maxage}; o front mostra "atualizado há…" pelo {@code atualizadoEm}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CabecalhosDeCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String caminho = request.getRequestURI();
        if (caminho.startsWith("/api/admin/") || caminho.startsWith("/api/auth/")) {
            response.setHeader("Cache-Control", "no-store");
        } else if (caminho.startsWith("/api/public/")) {
            response.setHeader("Cache-Control", "no-cache");
        }
        chain.doFilter(request, response);
    }
}
