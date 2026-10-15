package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "decisao_desempate")
@Getter
@Setter
@NoArgsConstructor
public class DecisaoDesempateEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "fase_id", nullable = false)
    private UUID faseId;

    @Column(name = "times_ids", nullable = false)
    private UUID[] timesIds;

    @Column(name = "ordem_ids", nullable = false)
    private UUID[] ordemIds;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "admin_id")
    private UUID adminId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();
}
