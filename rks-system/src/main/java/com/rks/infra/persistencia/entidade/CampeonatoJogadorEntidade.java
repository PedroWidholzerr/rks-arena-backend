package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Garante um Jogador por Campeonato (PK composta). */
@Entity
@Table(name = "campeonato_jogador")
@IdClass(CampeonatoJogadorId.class)
@Getter
@Setter
@NoArgsConstructor
public class CampeonatoJogadorEntidade {

    @Id
    @Column(name = "campeonato_id")
    private UUID campeonatoId;

    @Id
    @Column(name = "jogador_id")
    private UUID jogadorId;

    public CampeonatoJogadorEntidade(UUID campeonatoId, UUID jogadorId) {
        this.campeonatoId = campeonatoId;
        this.jogadorId = jogadorId;
    }
}
