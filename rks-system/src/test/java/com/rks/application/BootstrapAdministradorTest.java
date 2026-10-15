package com.rks.application;

import com.rks.application.administradores.BootstrapAdministrador;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapAdministradorTest {

    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private UsuarioRepositorio usuarios;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepositorio.class);
    }

    private BootstrapAdministrador com(String email, String senha, boolean redefinir) {
        return new BootstrapAdministrador(usuarios, codificador, email, senha, redefinir);
    }

    private void executar(BootstrapAdministrador bootstrap) {
        bootstrap.run(new DefaultApplicationArguments());
    }

    @Test
    void semAdminAtivoCriaOPrimeiroComHashBcryptETrocaObrigatoria() {
        when(usuarios.countByAtivoTrue()).thenReturn(0L);
        when(usuarios.buscarPorEmail("eu@rks.com")).thenReturn(Optional.empty());

        executar(com("eu@rks.com", "senha-inicial-123", false));

        ArgumentCaptor<UsuarioEntidade> criado = ArgumentCaptor.forClass(UsuarioEntidade.class);
        verify(usuarios).save(criado.capture());
        assertEquals("eu@rks.com", criado.getValue().getEmail());
        assertTrue(criado.getValue().isTrocarSenha());
        assertTrue(criado.getValue().getSenhaHash().startsWith("$2"));
        assertTrue(codificador.matches("senha-inicial-123", criado.getValue().getSenhaHash()));
    }

    @Test
    void comAdminAtivoAsVariaveisSaoIgnoradas() {
        when(usuarios.countByAtivoTrue()).thenReturn(1L);

        executar(com("eu@rks.com", "senha-inicial-123", false));

        verify(usuarios, never()).save(any());
    }

    @Test
    void semVariaveisNaoCriaNada() {
        when(usuarios.countByAtivoTrue()).thenReturn(0L);

        executar(com("", "", false));
        executar(com("eu@rks.com", "curta", false));

        verify(usuarios, never()).save(any());
    }

    @Test
    void emailJaExistenteDesativadoNaoESobrescrito() {
        when(usuarios.countByAtivoTrue()).thenReturn(0L);
        when(usuarios.buscarPorEmail("eu@rks.com")).thenReturn(Optional.of(new UsuarioEntidade("eu@rks.com", "h", false)));

        executar(com("eu@rks.com", "senha-inicial-123", false));

        verify(usuarios, never()).save(any());
    }

    @Test
    void redefinirNaSubidaTrocaASenhaDoUnicoAdmin() {
        UsuarioEntidade unico = new UsuarioEntidade("eu@rks.com", codificador.encode("antiga-senha-123"), false);
        when(usuarios.countByAtivoTrue()).thenReturn(1L);
        when(usuarios.findByAtivoTrue()).thenReturn(List.of(unico));

        executar(com("", "nova-senha-reset-1", true));

        assertTrue(codificador.matches("nova-senha-reset-1", unico.getSenhaHash()));
        assertTrue(unico.isTrocarSenha());
        verify(usuarios).save(unico);
    }

    @Test
    void redefinirNaSubidaNaoMexeQuandoHaMaisDeUmAdmin() {
        UsuarioEntidade a = new UsuarioEntidade("a@rks.com", "ha", false);
        UsuarioEntidade b = new UsuarioEntidade("b@rks.com", "hb", false);
        when(usuarios.countByAtivoTrue()).thenReturn(2L);
        when(usuarios.findByAtivoTrue()).thenReturn(List.of(a, b));

        executar(com("", "nova-senha-reset-1", true));

        verify(usuarios, never()).save(any());
        assertFalse(a.isTrocarSenha());
        assertEquals("ha", a.getSenhaHash());
    }
}
