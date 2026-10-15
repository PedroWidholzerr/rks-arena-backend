package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "elenco")
@Getter
@Setter
@NoArgsConstructor
public class ElencoEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "time_id", nullable = false)
    private UUID timeId;

    @Column(name = "temporada_id", nullable = false)
    private UUID temporadaId;
}
