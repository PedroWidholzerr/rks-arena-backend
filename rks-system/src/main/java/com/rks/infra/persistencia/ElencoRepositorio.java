package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.ElencoEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElencoRepositorio extends JpaRepository<ElencoEntidade, UUID> {

    Optional<ElencoEntidade> findByTimeIdAndTemporadaId(UUID timeId, UUID temporadaId);

    List<ElencoEntidade> findByTimeIdOrderByTemporadaId(UUID timeId);
}
