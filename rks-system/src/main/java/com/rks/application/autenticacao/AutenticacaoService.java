package com.rks.application.autenticacao;

import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import com.rks.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AutenticacaoService {

    public static final int TAMANHO_MINIMO_SENHA = 10;

    /** Hash de uma senha qualquer, comparado quando o e-mail não existe, para o tempo não revelar o e-mail. */
    private static final String SENHA_FALSA = "senha-falsa-para-igualar-o-tempo";

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final JwtService jwt;
    private final String hashFalso;

    public AutenticacaoService(UsuarioRepositorio usuarios, PasswordEncoder codificador, JwtService jwt) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.hashFalso = codificador.encode(SENHA_FALSA);
    }

    public record Sessao(String token, boolean trocarSenha, Instant expiraEm) {
    }

    /** Credencial inválida responde sempre igual, sem dizer se errou o e-mail ou a senha. */
    @Transactional(readOnly = true)
    public Sessao entrar(String email, String senha) {
        UsuarioEntidade usuario = usuarios.buscarPorEmail(email == null ? "" : email.strip()).orElse(null);
        String hash = usuario != null ? usuario.getSenhaHash() : hashFalso;
        boolean confere = codificador.matches(senha == null ? "" : senha, hash);
        if (usuario == null || !usuario.isAtivo() || !confere) {
            throw credenciaisInvalidas();
        }
        return sessaoDe(usuario);
    }

    /** Troca a senha de quem está logado e libera o restante da API. */
    @Transactional
    public Sessao trocarSenha(String email, String senhaAtual, String novaSenha) {
        UsuarioEntidade usuario = usuarios.buscarPorEmail(email)
                .filter(UsuarioEntidade::isAtivo)
                .orElseThrow(AutenticacaoService::credenciaisInvalidas);
        if (!codificador.matches(senhaAtual == null ? "" : senhaAtual, usuario.getSenhaHash())) {
            throw credenciaisInvalidas();
        }
        if (novaSenha == null || novaSenha.length() < TAMANHO_MINIMO_SENHA) {
            throw ErroDeRegra.regra("SENHA_FRACA", "A nova senha precisa de pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        if (codificador.matches(novaSenha, usuario.getSenhaHash())) {
            throw ErroDeRegra.regra("SENHA_REPETIDA", "A nova senha precisa ser diferente da atual.");
        }
        usuario.setSenhaHash(codificador.encode(novaSenha));
        usuario.setTrocarSenha(false);
        usuarios.save(usuario);
        return sessaoDe(usuario);
    }

    private Sessao sessaoDe(UsuarioEntidade usuario) {
        return new Sessao(jwt.generateToken(usuario.getEmail()), usuario.isTrocarSenha(),
                Instant.now().plusMillis(jwt.expiracaoEmMillis()));
    }

    private static ErroDeRegra credenciaisInvalidas() {
        return new ErroDeRegra(ErroDeRegra.Tipo.NAO_AUTENTICADO, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.");
    }
}
