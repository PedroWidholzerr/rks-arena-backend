package com.rks.infra.persistencia.entidade;

import java.io.Serializable;
import java.util.UUID;

public record CampeonatoJogadorId(UUID campeonatoId, UUID jogadorId) implements Serializable {
}
