package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.AuditLogEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepositorio extends JpaRepository<AuditLogEntidade, UUID> {

    List<AuditLogEntidade> findByEntidadeAndEntidadeIdOrderByEmAsc(String entidade, UUID entidadeId);
}
