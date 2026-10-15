package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.TimeEntidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeRepositorio extends JpaRepository<TimeEntidade, UUID> {

    Optional<TimeEntidade> findByTagKey(String tagKey);

    List<TimeEntidade> findAllByOrderByNomeAsc();

    @Query("""
            select count(p) from PartidaEntidade p
            where p.estado = 'FINALIZADA' and (p.timeAId = :timeId or p.timeBId = :timeId)
            """)
    long contarPartidasFinalizadas(UUID timeId);
}
