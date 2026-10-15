package com.rks.domain.standings;

import com.rks.domain.rules.EstadoPartida;

import java.util.UUID;

/**
 * Visão da Partida de que a classificação precisa. Só {@link EstadoPartida#FINALIZADA} conta;
 * a rodada serve a quem filtra "até a rodada k" antes de chamar o cálculo.
 */
public record PartidaResultado(
        UUID timeA, UUID timeB, int jogosA, int jogosB, boolean walkover, EstadoPartida estado, int rodada) {
}
