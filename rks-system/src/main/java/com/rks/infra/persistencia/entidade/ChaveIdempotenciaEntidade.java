package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Resposta guardada por 24 h para repetir a mesma resposta a uma chave repetida (AD-23). */
@Entity
@Table(name = "chave_idempotencia")
@IdClass(ChaveIdempotenciaId.class)
@Getter
@Setter
@NoArgsConstructor
public class ChaveIdempotenciaEntidade {

    @Id
    private UUID chave;

    @Id
    private String escopo;

    @Column(nullable = false)
    private short status;

    @JdbcTypeCode(SqlTypes.JSON)
    private String corpo;

    @Column(nullable = false)
    private Instant em = Instant.now();
}
