package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.DecisaoDesempateEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DecisaoDesempateRepositorio extends JpaRepository<DecisaoDesempateEntidade, UUID> {

    List<DecisaoDesempateEntidade> findByFaseIdOrderByCriadoEmAsc(UUID faseId);
}
