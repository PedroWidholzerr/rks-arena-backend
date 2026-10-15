package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.FaseEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaseRepositorio extends JpaRepository<FaseEntidade, UUID> {

    List<FaseEntidade> findByCampeonatoIdOrderByOrdem(UUID campeonatoId);
}
