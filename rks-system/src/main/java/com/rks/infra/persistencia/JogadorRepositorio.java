package com.rks.infra.persistencia;

import com.rks.infra.persistencia.entidade.JogadorEntidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JogadorRepositorio extends JpaRepository<JogadorEntidade, UUID> {

    Optional<JogadorEntidade> findByRiotIdKey(String riotIdKey);

    /** Busca por Riot ID (nick ou tag), sem diferenciar maiúsculas. Texto vazio lista todos. */
    @Query("""
            select j from JogadorEntidade j
            where :busca = '' or j.riotIdKey like concat('%', :busca, '%')
            order by lower(j.nick), j.tag
            """)
    List<JogadorEntidade> buscar(String busca);

    @Query("""
            select count(p) from PartidaEntidade p
            where p.mvpJogadorId = :jogadorId and p.estado = 'FINALIZADA'
            """)
    long contarPartidasComMvp(UUID jogadorId);
}
