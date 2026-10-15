package com.rks.domain.rules;

/** Estados persistidos de uma Partida (AD-6). Só FINALIZADA entra na classificação. */
public enum EstadoPartida {
    AGENDADA,
    EM_ANDAMENTO,
    FINALIZADA,
    ANULADA
}
