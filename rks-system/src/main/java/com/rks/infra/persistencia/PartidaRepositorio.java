package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.PartidaEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartidaRepositorio extends JpaRepository<PartidaEntidade, UUID> {

    List<PartidaEntidade> findByFaseIdOrderByRodadaAscPosicaoAsc(UUID faseId);

    List<PartidaEntidade> findByFaseIdAndEstado(UUID faseId, String estado);

    long countByFaseId(UUID faseId);
}
