package com.rks.application;

import com.rks.application.administradores.AdministradorService;
import com.rks.application.auditoria.AuditoriaService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdministradorServiceTest {

    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private UsuarioRepositorio usuarios;
    private AuditoriaService auditoria;
    private AdministradorService servico;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepositorio.class);
        auditoria = mock(AuditoriaService.class);
        servico = new AdministradorService(usuarios, codificador, auditoria);
        when(usuarios.save(any(UsuarioEntidade.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void criaAdministradorComSenhaTemporariaETrocaObrigatoria() {
        when(usuarios.countByAtivoTrue()).thenReturn(1L);
        when(usuarios.buscarPorEmail("novo@rks.com")).thenReturn(Optional.empty());

        AdministradorService.Criado criado = servico.criar("eu@rks.com", " novo@rks.com ");

        assertEquals("novo@rks.com", criado.administrador().email());
        assertTrue(criado.administrador().trocarSenha());
        assertEquals(16, criado.senhaTemporaria().length());
        ArgumentCaptor<UsuarioEntidade> salvo = ArgumentCaptor.forClass(UsuarioEntidade.class);
        verify(usuarios).save(salvo.capture());
        assertTrue(codificador.matches(criado.senhaTemporaria(), salvo.getValue().getSenhaHash()));
        assertNotEquals(criado.senhaTemporaria(), salvo.getValue().getSenhaHash());
        verify(auditoria).registrar(eq("eu@rks.com"), eq("usuario"), any(UUID.class), eq("CRIAR"), eq(null), any());
    }

    @Test
    void travaAntesDeContarOsAtivos() {
        when(usuarios.countByAtivoTrue()).thenReturn(1L);
        when(usuarios.buscarPorEmail(any())).thenReturn(Optional.empty());

        servico.criar("eu@rks.com", "novo@rks.com");

        var ordem = inOrder(usuarios);
        ordem.verify(usuarios).travar(anyLong());
        ordem.verify(usuarios).countByAtivoTrue();
    }

    @Test
    void oQuartoAdministradorAtivoERecusado() {
        when(usuarios.countByAtivoTrue()).thenReturn(3L);

        ErroDeRegra erro = assertThrows(ErroDeRegra.class, () -> servico.criar("eu@rks.com", "quarto@rks.com"));

        assertEquals("LIMITE_DE_ADMINISTRADORES", erro.codigo());
        verify(usuarios, never()).save(any());
    }

    @Test
    void emailInvalidoOuRepetidoERecusado() {
        when(usuarios.countByAtivoTrue()).thenReturn(1L);
        when(usuarios.buscarPorEmail("repetido@rks.com"))
                .thenReturn(Optional.of(new UsuarioEntidade("repetido@rks.com", "h", false)));

        assertEquals("EMAIL_INVALIDO", assertThrows(ErroDeRegra.class, () -> servico.criar("eu", "sem-arroba")).codigo());
        assertEquals("EMAIL_JA_CADASTRADO",
                assertThrows(ErroDeRegra.class, () -> servico.criar("eu", "repetido@rks.com")).codigo());
    }

    @Test
    void desativarOUltimoAdminAtivoERecusado() {
        UsuarioEntidade ultimo = new UsuarioEntidade("eu@rks.com", "h", false);
        when(usuarios.findById(ultimo.getId())).thenReturn(Optional.of(ultimo));
        when(usuarios.countByAtivoTrue()).thenReturn(1L);

        ErroDeRegra erro = assertThrows(ErroDeRegra.class, () -> servico.definirAtivo("eu@rks.com", ultimo.getId(), false));

        assertEquals("ULTIMO_ADMINISTRADOR", erro.codigo());
        assertTrue(ultimo.isAtivo());
    }

    @Test
    void desativaQuandoHaOutroAtivoEAuditaSemExcluir() {
        UsuarioEntidade alvo = new UsuarioEntidade("b@rks.com", "h", false);
        when(usuarios.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarios.countByAtivoTrue()).thenReturn(2L);

        AdministradorService.Administrador resultado = servico.definirAtivo("a@rks.com", alvo.getId(), false);

        assertFalse(resultado.ativo());
        verify(usuarios, never()).delete(any());
        verify(usuarios, never()).deleteById(any());
        verify(auditoria).registrar(eq("a@rks.com"), eq("usuario"), eq(alvo.getId()), eq("DESATIVAR"), any(), any());
    }

    @Test
    void reativarRespeitaOTetoDeAtivos() {
        UsuarioEntidade inativo = new UsuarioEntidade("c@rks.com", "h", false);
        inativo.setAtivo(false);
        when(usuarios.findById(inativo.getId())).thenReturn(Optional.of(inativo));
        when(usuarios.countByAtivoTrue()).thenReturn(3L);

        ErroDeRegra erro = assertThrows(ErroDeRegra.class, () -> servico.definirAtivo("a@rks.com", inativo.getId(), true));

        assertEquals("LIMITE_DE_ADMINISTRADORES", erro.codigo());
    }

    @Test
    void administradorInexistenteDa404() {
        UUID id = UUID.randomUUID();
        when(usuarios.findById(id)).thenReturn(Optional.empty());

        ErroDeRegra erro = assertThrows(ErroDeRegra.class, () -> servico.definirAtivo("a@rks.com", id, false));

        assertEquals(ErroDeRegra.Tipo.NAO_ENCONTRADO, erro.tipo());
    }

    @Test
    void administradorDefineOProprioNomeSemEspacosSobrando() {
        UsuarioEntidade eu = new UsuarioEntidade("eu@rks.com", "h", false);
        when(usuarios.buscarPorEmail("eu@rks.com")).thenReturn(Optional.of(eu));

        AdministradorService.Administrador resultado = servico.atualizarNome("eu@rks.com", "  Pedro   Costa  ");

        assertEquals("Pedro Costa", resultado.nome());
        assertEquals("Pedro Costa", eu.getNome());
        verify(auditoria).registrar(eq("eu@rks.com"), eq("usuario"), eq(eu.getId()), eq("NOME"), any(), any());
    }

    @Test
    void nomeVazioVoltaAMostrarOEmail() {
        UsuarioEntidade eu = new UsuarioEntidade("eu@rks.com", "h", false);
        eu.setNome("Antigo");
        when(usuarios.buscarPorEmail("eu@rks.com")).thenReturn(Optional.of(eu));

        assertEquals(null, servico.atualizarNome("eu@rks.com", "   ").nome());
        assertEquals(null, AdministradorService.nomeLimpo(null));
    }

    @Test
    void nomeCom121CaracteresEhRecusado() {
        assertEquals("NOME_INVALIDO", assertThrows(ErroDeRegra.class, () -> AdministradorService.nomeLimpo("a".repeat(121))).codigo());
    }

    @Test
    void criaAdministradorJaComNome() {
        when(usuarios.countByAtivoTrue()).thenReturn(1L);
        when(usuarios.buscarPorEmail("novo@rks.com")).thenReturn(Optional.empty());

        AdministradorService.Criado criado = servico.criar("eu@rks.com", "novo@rks.com", "Maria");

        assertEquals("Maria", criado.administrador().nome());
    }
}
