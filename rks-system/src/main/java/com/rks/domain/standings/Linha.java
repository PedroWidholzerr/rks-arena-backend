package com.rks.domain.standings;

import java.util.UUID;

/**
 * Linha da tabela. Times empatados sem decisão dividem a mesma posição e saem com
 * {@code empate = true}.
 */
public record Linha(
        int posicao,
        UUID timeId,
        int jogadas,
        int vitorias,
        int derrotas,
        int jogosAFavor,
        int jogosContra,
        int saldo,
        int pontos,
        boolean empate) {
}
