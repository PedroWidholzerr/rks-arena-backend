package com.rks.domain.standings;

/** Critérios 2 a 4 do FR-13; o administrador pode reordená-los por Campeonato. */
public enum CriterioDesempate {
    /** Pontos só nas Partidas entre os empatados (mini-tabela). */
    CONFRONTO_DIRETO,
    /** Jogos a favor menos Jogos contra, em todas as Partidas. */
    SALDO,
    /** Total de Jogos vencidos (Jogos a favor), em todas as Partidas. */
    JOGOS_VENCIDOS
}
