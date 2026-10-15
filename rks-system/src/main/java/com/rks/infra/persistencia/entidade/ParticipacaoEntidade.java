package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Time no Campeonato (tabela participacao_campeonato). Não confundir com a Inscrição da O3. */
@Entity
@Table(name = "participacao_campeonato")
@Getter
@Setter
@NoArgsConstructor
public class ParticipacaoEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "campeonato_id", nullable = false)
    private UUID campeonatoId;

    @Column(name = "time_id", nullable = false)
    private UUID timeId;

    @Column(name = "elenco_id", nullable = false)
    private UUID elencoId;
}
