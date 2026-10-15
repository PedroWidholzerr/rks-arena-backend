package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.TemporadaEntidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TemporadaRepositorio extends JpaRepository<TemporadaEntidade, UUID> {

    /** Da mais recente para a mais antiga. */
    @Query("select t from TemporadaEntidade t order by coalesce(t.dataInicio, cast(t.criadoEm as localdate)) desc, t.criadoEm desc")
    List<TemporadaEntidade> listarDaMaisRecente();

    @Query("""
            select count(p) from PartidaEntidade p, FaseEntidade f, CampeonatoEntidade c
            where p.faseId = f.id and f.campeonatoId = c.id and c.temporadaId = :temporadaId
              and p.estado = 'FINALIZADA'
            """)
    long contarPartidasFinalizadas(UUID temporadaId);
}
