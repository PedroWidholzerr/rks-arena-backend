package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.ParticipacaoEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipacaoRepositorio extends JpaRepository<ParticipacaoEntidade, UUID> {

    List<ParticipacaoEntidade> findByCampeonatoId(UUID campeonatoId);

    List<ParticipacaoEntidade> findByTimeId(UUID timeId);

    Optional<ParticipacaoEntidade> findByCampeonatoIdAndTimeId(UUID campeonatoId, UUID timeId);

    List<ParticipacaoEntidade> findByElencoId(UUID elencoId);
}
