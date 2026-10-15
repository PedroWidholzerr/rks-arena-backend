package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.ChaveIdempotenciaEntidade;
import com.rks.infra.persistencia.entidade.ChaveIdempotenciaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChaveIdempotenciaRepositorio extends JpaRepository<ChaveIdempotenciaEntidade, ChaveIdempotenciaId> {

    List<ChaveIdempotenciaEntidade> findByChave(UUID chave);

    /** Trava de transação por chave: dois pedidos simultâneos com a mesma chave entram um de cada vez. */
    @Query(value = "select cast(pg_advisory_xact_lock(hashtextextended(:chave, 0)) as varchar)", nativeQuery = true)
    String travar(String chave);

    @Modifying
    @Query("delete from ChaveIdempotenciaEntidade c where c.em < :limite")
    int apagarAnterioresA(Instant limite);
}
