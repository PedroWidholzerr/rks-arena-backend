package com.rks.api.auth;

import com.rks.application.autenticacao.AutenticacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String email, @NotBlank String senha) {
    }

    public record TrocaSenhaRequest(@NotBlank String senhaAtual, @NotBlank String novaSenha) {
    }

    public record SessaoResponse(String token, boolean trocarSenha, Instant expiraEm, String nome) {
    }

    private final AutenticacaoService autenticacao;

    public AuthController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping("/login")
    public SessaoResponse entrar(@Valid @RequestBody LoginRequest pedido) {
        return paraResposta(autenticacao.entrar(pedido.email(), pedido.senha()));
    }

    @PostMapping("/password")
    public SessaoResponse trocarSenha(Authentication quem, @Valid @RequestBody TrocaSenhaRequest pedido) {
        return paraResposta(autenticacao.trocarSenha(quem.getName(), pedido.senhaAtual(), pedido.novaSenha()));
    }

    private static SessaoResponse paraResposta(AutenticacaoService.Sessao sessao) {
        return new SessaoResponse(sessao.token(), sessao.trocarSenha(), sessao.expiraEm(), sessao.nome());
    }
}
