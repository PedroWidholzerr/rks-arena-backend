package com.rks.application.auditoria;

import com.rks.infra.persistencia.AuditLogRepositorio;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.AuditLogEntidade;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Componente reutilizável de auditoria (AD-22, NFR-12). Só roda dentro da transação do caso de uso
 * que muda o dado ({@link Propagation#MANDATORY}): se ela falhar, o registro some junto; se ela
 * concluir, o registro fica. Quem chama passa apenas campos seguros, nunca senha nem token.
 */
@Service
public class AuditoriaService {

    private final AuditLogRepositorio registros;
    private final UsuarioRepositorio usuarios;
    private final ObjectMapper json;

    public AuditoriaService(AuditLogRepositorio registros, UsuarioRepositorio usuarios, ObjectMapper json) {
        this.registros = registros;
        this.usuarios = usuarios;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String emailDoAdmin, String entidade, UUID entidadeId, String acao,
                          Object antes, Object depois) {
        AuditLogEntidade registro = new AuditLogEntidade();
        registro.setAdminId(emailDoAdmin == null ? null
                : usuarios.buscarPorEmail(emailDoAdmin).map(UsuarioEntidade::getId).orElse(null));
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setAcao(acao);
        registro.setAntes(antes == null ? null : json.writeValueAsString(antes));
        registro.setDepois(depois == null ? null : json.writeValueAsString(depois));
        registros.save(registro);
    }
}
