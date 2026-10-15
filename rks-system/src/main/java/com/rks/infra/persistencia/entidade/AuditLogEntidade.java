package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Registro de auditoria (AD-22): quem mudou o quê, com o antes e o depois em JSON. */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private Instant em = Instant.now();

    @Column(name = "admin_id")
    private UUID adminId;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @Column(nullable = false)
    private String acao;

    @JdbcTypeCode(SqlTypes.JSON)
    private String antes;

    @JdbcTypeCode(SqlTypes.JSON)
    private String depois;
}
