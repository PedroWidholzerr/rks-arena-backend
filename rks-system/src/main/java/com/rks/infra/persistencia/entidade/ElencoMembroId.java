package com.rks.infra.persistencia.entidade;

import java.io.Serializable;
import java.util.UUID;

public record ElencoMembroId(UUID elencoId, UUID jogadorId) implements Serializable {
}
