package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositorio extends JpaRepository<UsuarioEntidade, UUID> {

    @Query("select u from UsuarioEntidade u where lower(u.email) = lower(:email)")
    Optional<UsuarioEntidade> buscarPorEmail(String email);

    long countByAtivoTrue();

    List<UsuarioEntidade> findAllByOrderByCriadoEmAsc();

    List<UsuarioEntidade> findByAtivoTrue();

    /** Trava de transação (AD-15): serializa criar e desativar administradores para o teto valer sob concorrência. */
    @Query(value = "select cast(pg_advisory_xact_lock(:chave) as varchar)", nativeQuery = true)
    String travar(long chave);
}
