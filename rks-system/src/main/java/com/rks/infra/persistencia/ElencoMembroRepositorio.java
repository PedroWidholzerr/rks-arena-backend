package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.ElencoMembroEntidade;
import com.rks.infra.persistencia.entidade.ElencoMembroId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ElencoMembroRepositorio extends JpaRepository<ElencoMembroEntidade, ElencoMembroId> {

    List<ElencoMembroEntidade> findByElencoId(UUID elencoId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ElencoMembroEntidade m where m.elencoId = :elencoId")
    void apagarDoElenco(UUID elencoId);
}
