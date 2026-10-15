package com.rks.domain.standings;

import com.rks.domain.rules.EstadoPartida;

import java.util.List;
import java.util.UUID;

/** Atalhos para montar Times e Partidas nos testes da classificação. */
final class Dados {

    static final UUID A = t(1);
    static final UUID B = t(2);
    static final UUID C = t(3);
    static final UUID D = t(4);
    static final UUID E = t(5);

    private Dados() {
    }

    static UUID t(int n) {
        return new UUID(0L, n);
    }

    /** Partida FINALIZADA sem W.O. */
    static PartidaResultado fin(UUID a, UUID b, int jogosA, int jogosB) {
        return new PartidaResultado(a, b, jogosA, jogosB, false, EstadoPartida.FINALIZADA, 1);
    }

    static PartidaResultado wo(UUID a, UUID b, int jogosA, int jogosB) {
        return new PartidaResultado(a, b, jogosA, jogosB, true, EstadoPartida.FINALIZADA, 1);
    }

    static PartidaResultado emEstado(EstadoPartida estado, UUID a, UUID b) {
        boolean finalizada = estado == EstadoPartida.FINALIZADA;
        return new PartidaResultado(a, b, finalizada ? 2 : 0, 0, false, estado, 1);
    }

    static List<UUID> ordem(Classificacao c) {
        return c.linhas().stream().map(Linha::timeId).toList();
    }

    static Linha linhaDe(Classificacao c, UUID time) {
        return c.linhas().stream().filter(l -> l.timeId().equals(time)).findFirst().orElseThrow();
    }
}
