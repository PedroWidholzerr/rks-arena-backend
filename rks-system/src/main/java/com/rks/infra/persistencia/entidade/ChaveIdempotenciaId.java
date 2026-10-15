package com.rks.infra.persistencia.entidade;

import java.io.Serializable;
import java.util.UUID;

public record ChaveIdempotenciaId(UUID chave, String escopo) implements Serializable {
}
