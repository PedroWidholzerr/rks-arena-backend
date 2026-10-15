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

import java.util.UUID;

@Entity
@Table(name = "fase")
@Getter
@Setter
@NoArgsConstructor
public class FaseEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "campeonato_id", nullable = false)
    private UUID campeonatoId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String formato = "PONTOS_CORRIDOS";

    @Column(nullable = false)
    private short ordem = 1;

    @Column(nullable = false)
    private short turnos = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String params = "{}";
}
