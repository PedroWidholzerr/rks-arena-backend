package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.CampeonatoJogadorEntidade;
import com.rks.infra.persistencia.entidade.CampeonatoJogadorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CampeonatoJogadorRepositorio extends JpaRepository<CampeonatoJogadorEntidade, CampeonatoJogadorId> {

    List<CampeonatoJogadorEntidade> findByCampeonatoId(UUID campeonatoId);

    boolean existsByCampeonatoIdAndJogadorId(UUID campeonatoId, UUID jogadorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CampeonatoJogadorEntidade c where c.campeonatoId = :campeonatoId and c.jogadorId in :jogadores")
    void apagar(UUID campeonatoId, Collection<UUID> jogadores);
}
