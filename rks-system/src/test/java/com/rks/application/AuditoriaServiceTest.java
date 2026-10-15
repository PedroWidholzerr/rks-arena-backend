package com.rks.application;

import com.rks.application.auditoria.AuditoriaService;
import com.rks.infra.persistencia.AuditLogRepositorio;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.AuditLogEntidade;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditoriaServiceTest {

    @Test
    void gravaQuemQuandoEntidadeAcaoAntesEDepois() {
        AuditLogRepositorio registros = mock(AuditLogRepositorio.class);
        UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
        UsuarioEntidade admin = new UsuarioEntidade("admin@rks.com", "h", false);
        when(usuarios.buscarPorEmail("admin@rks.com")).thenReturn(Optional.of(admin));
        AuditoriaService auditoria = new AuditoriaService(registros, usuarios, JsonMapper.builder().build());
        UUID partida = UUID.randomUUID();

        auditoria.registrar("admin@rks.com", "partida", partida, "RESULTADO",
                Map.of("placar", "0x0"), Map.of("placar", "2x1"));

        ArgumentCaptor<AuditLogEntidade> salvo = ArgumentCaptor.forClass(AuditLogEntidade.class);
        verify(registros).save(salvo.capture());
        AuditLogEntidade registro = salvo.getValue();
        assertEquals(admin.getId(), registro.getAdminId());
        assertEquals("partida", registro.getEntidade());
        assertEquals(partida, registro.getEntidadeId());
        assertEquals("RESULTADO", registro.getAcao());
        assertEquals("{\"placar\":\"0x0\"}", registro.getAntes());
        assertEquals("{\"placar\":\"2x1\"}", registro.getDepois());
        assertNotNull(registro.getEm());
    }

    @Test
    void antesEDepoisPodemSerNulos() {
        AuditLogRepositorio registros = mock(AuditLogRepositorio.class);
        AuditoriaService auditoria = new AuditoriaService(registros, mock(UsuarioRepositorio.class),
                JsonMapper.builder().build());

        auditoria.registrar(null, "usuario", null, "CRIAR", null, null);

        ArgumentCaptor<AuditLogEntidade> salvo = ArgumentCaptor.forClass(AuditLogEntidade.class);
        verify(registros).save(salvo.capture());
        assertNull(salvo.getValue().getAntes());
        assertNull(salvo.getValue().getAdminId());
    }

    @Test
    void soRodaDentroDaTransacaoDeQuemChama() throws Exception {
        Transactional anotacao = AuditoriaService.class
                .getMethod("registrar", String.class, String.class, UUID.class, String.class, Object.class, Object.class)
                .getAnnotation(Transactional.class);

        assertNotNull(anotacao);
        assertTrue(anotacao.propagation() == Propagation.MANDATORY,
                "A auditoria precisa entrar na transação do caso de uso, para sumir junto se ela falhar.");
    }
}
