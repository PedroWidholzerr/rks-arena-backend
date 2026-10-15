package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "temporada")
@Getter
@Setter
@NoArgsConstructor
public class TemporadaEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nome;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(nullable = false)
    private boolean encerrada;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();
}
