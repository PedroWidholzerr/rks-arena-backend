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
@Table(name = "campeonato")
@Getter
@Setter
@NoArgsConstructor
public class CampeonatoEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "temporada_id", nullable = false)
    private UUID temporadaId;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private String modo = "ARAM";

    @Column(name = "tamanho_time", nullable = false)
    private short tamanhoTime = 3;

    @Column(name = "melhor_de", nullable = false)
    private short melhorDe = 1;

    @Column(name = "pontos_vitoria", nullable = false)
    private short pontosVitoria = 2;

    @Column(name = "pontos_derrota_decisiva", nullable = false)
    private short pontosDerrotaDecisiva = 1;

    @Column(name = "pontos_derrota", nullable = false)
    private short pontosDerrota = 0;

    @Column(name = "limite_times")
    private Short limiteTimes;

    @Column(name = "ordem_desempate", nullable = false)
    private String ordemDesempate = "CONFRONTO_DIRETO,SALDO,JOGOS_VENCIDOS";

    @Column(nullable = false)
    private String estado = "EM_BREVE";

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();
}
