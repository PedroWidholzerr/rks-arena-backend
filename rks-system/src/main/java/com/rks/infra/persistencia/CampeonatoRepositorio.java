package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.CampeonatoEntidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampeonatoRepositorio extends JpaRepository<CampeonatoEntidade, UUID> {

    List<CampeonatoEntidade> findByTemporadaIdOrderByCriadoEmDesc(UUID temporadaId);

    /** SELECT ... FOR UPDATE: a mesma trava serve a editar a configuração e a lançar resultado (AD-10). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampeonatoEntidade c where c.id = :id")
    Optional<CampeonatoEntidade> buscarParaAtualizar(UUID id);

    @Query("""
            select count(p) from PartidaEntidade p, FaseEntidade f
            where p.faseId = f.id and f.campeonatoId = :campeonatoId and p.estado = 'FINALIZADA'
            """)
    long contarPartidasFinalizadas(UUID campeonatoId);
}
