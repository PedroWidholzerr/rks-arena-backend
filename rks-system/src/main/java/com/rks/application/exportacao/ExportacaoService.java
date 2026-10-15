package com.rks.application.exportacao;

import com.rks.infra.persistencia.AuditLogRepositorio;
import com.rks.infra.persistencia.CampeonatoJogadorRepositorio;
import com.rks.infra.persistencia.CampeonatoRepositorio;
import com.rks.infra.persistencia.DecisaoDesempateRepositorio;
import com.rks.infra.persistencia.ElencoMembroRepositorio;
import com.rks.infra.persistencia.ElencoRepositorio;
import com.rks.infra.persistencia.FaseRepositorio;
import com.rks.infra.persistencia.JogadorRepositorio;
import com.rks.infra.persistencia.ParticipacaoRepositorio;
import com.rks.infra.persistencia.PartidaRepositorio;
import com.rks.infra.persistencia.TemporadaRepositorio;
import com.rks.infra.persistencia.TimeRepositorio;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exportação completa em JSON (NFR-5): o administrador guarda uma cópia fora do sistema. Reúne todas as
 * tabelas de dados, e NUNCA hash de senha nem token. As imagens ficam no armazenamento externo; aqui
 * vai só a chave de cada uma.
 */
@Service
public class ExportacaoService {

    public record UsuarioExportado(UUID id, String email, boolean ativo, Instant criadoEm) {
    }

    private final TemporadaRepositorio temporadas;
    private final CampeonatoRepositorio campeonatos;
    private final FaseRepositorio fases;
    private final TimeRepositorio times;
    private final JogadorRepositorio jogadores;
    private final ElencoRepositorio elencos;
    private final ElencoMembroRepositorio membros;
    private final ParticipacaoRepositorio participacoes;
    private final CampeonatoJogadorRepositorio campeonatoJogadores;
    private final PartidaRepositorio partidas;
    private final DecisaoDesempateRepositorio decisoes;
    private final AuditLogRepositorio auditoria;
    private final UsuarioRepositorio usuarios;

    public ExportacaoService(TemporadaRepositorio temporadas, CampeonatoRepositorio campeonatos, FaseRepositorio fases,
                             TimeRepositorio times, JogadorRepositorio jogadores, ElencoRepositorio elencos,
                             ElencoMembroRepositorio membros, ParticipacaoRepositorio participacoes,
                             CampeonatoJogadorRepositorio campeonatoJogadores, PartidaRepositorio partidas,
                             DecisaoDesempateRepositorio decisoes, AuditLogRepositorio auditoria,
                             UsuarioRepositorio usuarios) {
        this.temporadas = temporadas;
        this.campeonatos = campeonatos;
        this.fases = fases;
        this.times = times;
        this.jogadores = jogadores;
        this.elencos = elencos;
        this.membros = membros;
        this.participacoes = participacoes;
        this.campeonatoJogadores = campeonatoJogadores;
        this.partidas = partidas;
        this.decisoes = decisoes;
        this.auditoria = auditoria;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportar() {
        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("geradoEm", Instant.now());
        saida.put("formato", 1);
        saida.put("temporadas", temporadas.findAll());
        saida.put("campeonatos", campeonatos.findAll());
        saida.put("fases", fases.findAll());
        saida.put("times", times.findAll());
        saida.put("jogadores", jogadores.findAll());
        saida.put("elencos", elencos.findAll());
        saida.put("elencoMembros", membros.findAll());
        saida.put("participacoes", participacoes.findAll());
        saida.put("campeonatoJogadores", campeonatoJogadores.findAll());
        saida.put("partidas", partidas.findAll());
        saida.put("decisoesDesempate", decisoes.findAll());
        saida.put("auditoria", auditoria.findAll());
        List<UsuarioExportado> admins = usuarios.findAllByOrderByCriadoEmAsc().stream()
                .map((UsuarioEntidade u) -> new UsuarioExportado(u.getId(), u.getEmail(), u.isAtivo(), u.getCriadoEm()))
                .toList();
        saida.put("administradores", admins);
        return saida;
    }
}
