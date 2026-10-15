package com.rks.domain.standings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Decisão do administrador para um empate que nenhum critério separa (critério 5 do FR-13).
 * Guarda a ordem final dos Times e o motivo. Só se aplica se o conjunto de empatados
 * ainda não resolvidos for exatamente o conjunto guardado.
 */
public record DecisaoManual(List<UUID> ordem, String motivo) {

    public DecisaoManual {
        if (ordem == null || ordem.size() < 2 || new HashSet<>(ordem).size() != ordem.size()) {
            throw new IllegalArgumentException("A decisão manual exige ao menos 2 Times distintos.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("A decisão manual exige um motivo.");
        }
        ordem = List.copyOf(ordem);
    }

    /** Conjunto de Times a que a decisão se refere. */
    public Set<UUID> conjunto() {
        return new HashSet<>(ordem);
    }
}
