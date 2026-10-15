package com.rks.security;

import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica pelo JWT e confirma o usuário no banco a cada requisição: administrador desativado
 * perde o acesso na hora. Quem ainda precisa trocar a senha recebe só o papel SENHA_PENDENTE, que
 * abre apenas /api/auth/password; o papel ADMIN só vem depois da troca.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String PAPEL_ADMIN = "ROLE_ADMIN";
    public static final String PAPEL_SENHA_PENDENTE = "ROLE_SENHA_PENDENTE";

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null && jwtService.isTokenValid(token)) {
                usuarios.buscarPorEmail(jwtService.extractEmail(token))
                        .filter(UsuarioEntidade::isAtivo)
                        .ifPresent(usuario -> autenticar(request, usuario));
            }
        } catch (RuntimeException e) {
            logger.error("Não foi possível autenticar a requisição", e);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request, UsuarioEntidade usuario) {
        String papel = usuario.isTrocarSenha() ? PAPEL_SENHA_PENDENTE : PAPEL_ADMIN;
        UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
                usuario.getEmail(), null, List.of(new SimpleGrantedAuthority(papel)));
        autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
