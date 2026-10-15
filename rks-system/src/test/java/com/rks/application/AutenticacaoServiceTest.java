package com.rks.application;

import com.rks.application.autenticacao.AutenticacaoService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import com.rks.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutenticacaoServiceTest {

    private static final String EMAIL = "admin@rks.com";
    private static final String SENHA = "senha-correta-123";

    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private UsuarioRepositorio usuarios;
    private JwtService jwt;
    private AutenticacaoService servico;

    @BeforeEach
    void preparar() throws Exception {
        usuarios = mock(UsuarioRepositorio.class);
        jwt = new JwtService();
        campo(jwt, "secretKey", "segredo-somente-para-testes-com-mais-de-32-bytes");
        campo(jwt, "jwtExpiration", 12L * 60 * 60 * 1000);
        jwt.init();
        servico = new AutenticacaoService(usuarios, codificador, jwt);
    }

    private UsuarioEntidade usuario(boolean ativo, boolean trocarSenha) {
        UsuarioEntidade u = new UsuarioEntidade(EMAIL, codificador.encode(SENHA), trocarSenha);
        u.setAtivo(ativo);
        when(usuarios.buscarPorEmail(EMAIL)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void loginValidoDevolveTokenDe12Horas() {
        usuario(true, false);

        AutenticacaoService.Sessao sessao = servico.entrar(EMAIL, SENHA);

        assertTrue(jwt.isTokenValid(sessao.token()));
        assertEquals(EMAIL, jwt.extractEmail(sessao.token()));
        assertFalse(sessao.trocarSenha());
        assertEquals(12L * 60 * 60 * 1000, jwt.expiracaoEmMillis());
    }

    @Test
    void loginSinalizaTrocaObrigatoriaDeSenha() {
        usuario(true, true);

        assertTrue(servico.entrar(EMAIL, SENHA).trocarSenha());
    }

    @Test
    void credencialInvalidaNaoDizQualCampoErrou() {
        usuario(true, false);
        when(usuarios.buscarPorEmail("outro@rks.com")).thenReturn(Optional.empty());

        ErroDeRegra senhaErrada = assertThrows(ErroDeRegra.class, () -> servico.entrar(EMAIL, "senha-errada-123"));
        ErroDeRegra emailErrado = assertThrows(ErroDeRegra.class, () -> servico.entrar("outro@rks.com", SENHA));

        assertEquals(senhaErrada.getMessage(), emailErrado.getMessage());
        assertEquals(senhaErrada.codigo(), emailErrado.codigo());
        assertEquals(ErroDeRegra.Tipo.NAO_AUTENTICADO, senhaErrada.tipo());
    }

    @Test
    void administradorDesativadoNaoEntra() {
        usuario(false, false);

        assertThrows(ErroDeRegra.class, () -> servico.entrar(EMAIL, SENHA));
    }

    @Test
    void trocarSenhaLiberaOAcessoEGravaOHashNovo() {
        UsuarioEntidade u = usuario(true, true);

        AutenticacaoService.Sessao sessao = servico.trocarSenha(EMAIL, SENHA, "nova-senha-forte-9");

        assertFalse(sessao.trocarSenha());
        assertFalse(u.isTrocarSenha());
        assertTrue(codificador.matches("nova-senha-forte-9", u.getSenhaHash()));
        verify(usuarios).save(u);
    }

    @Test
    void trocarSenhaExigeSenhaAtualCorreta() {
        usuario(true, true);

        assertThrows(ErroDeRegra.class, () -> servico.trocarSenha(EMAIL, "errada-errada-1", "nova-senha-forte-9"));
    }

    @Test
    void novaSenhaCurtaOuIgualARepetidaERejeitada() {
        usuario(true, true);

        ErroDeRegra curta = assertThrows(ErroDeRegra.class, () -> servico.trocarSenha(EMAIL, SENHA, "curta"));
        ErroDeRegra igual = assertThrows(ErroDeRegra.class, () -> servico.trocarSenha(EMAIL, SENHA, SENHA));

        assertEquals("SENHA_FRACA", curta.codigo());
        assertEquals("SENHA_REPETIDA", igual.codigo());
    }

    private static void campo(Object alvo, String nome, Object valor) throws Exception {
        Field f = alvo.getClass().getDeclaredField(nome);
        f.setAccessible(true);
        f.set(alvo, valor);
    }
}
