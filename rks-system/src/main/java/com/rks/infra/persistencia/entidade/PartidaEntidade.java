package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partida")
@Getter
@Setter
@NoArgsConstructor
public class PartidaEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "fase_id", nullable = false)
    private UUID faseId;

    @Column(nullable = false)
    private short rodada = 1;

    @Column(nullable = false)
    private short posicao = 1;

    @Column(name = "time_a_id")
    private UUID timeAId;

    @Column(name = "time_b_id")
    private UUID timeBId;

    @Column(nullable = false)
    private String estado = "AGENDADA";

    @Column(name = "placar_a")
    private Short placarA;

    @Column(name = "placar_b")
    private Short placarB;

    @Column(nullable = false)
    private boolean walkover;

    @Column(name = "agendada_para")
    private Instant agendadaPara;

    @Column(name = "stream_url")
    private String streamUrl;

    @Column(name = "mvp_jogador_id")
    private UUID mvpJogadorId;

    /** Controle otimista (AD-6): escrita com versão defasada vira 409. */
    @Version
    private long version;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();
}
