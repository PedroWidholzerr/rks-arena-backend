package com.rks.application.classificacao;

import com.rks.application.auditoria.AuditoriaService;
import com.rks.application.campeonatos.CampeonatoService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.application.imagens.ImageStorage;
import com.rks.domain.rules.EstadoPartida;
import com.rks.domain.standings.Classificacao;
import com.rks.domain.standings.DecisaoManual;
import com.rks.domain.standings.Linha;
import com.rks.domain.standings.PartidaResultado;
import com.rks.domain.standings.RegrasClassificacao;
import com.rks.domain.standings.Standings;
import com.rks.infra.persistencia.DecisaoDesempateRepositorio;
import com.rks.infra.persistencia.FaseRepositorio;
import com.rks.infra.persistencia.ParticipacaoRepositorio;
import com.rks.infra.persistencia.PartidaRepositorio;
import com.rks.infra.persistencia.TimeRepositorio;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.DecisaoDesempateEntidade;
import com.rks.infra.persistencia.entidade.FaseEntidade;
import com.rks.infra.persistencia.entidade.ParticipacaoEntidade;
import com.rks.infra.persistencia.entidade.PartidaEntidade;
import com.rks.infra.persistencia.entidade.TimeEntidade;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Classificação sempre derivada das Partidas (AD-2): a tabela vem de {@link Standings#compute} a
 * cada consulta, sem cache e sem tabela guardada. Nada aqui soma ou subtrai pontos de um estado
 * anterior. O front nunca calcula posição, pontos nem desempate.
 */
@Service
public class ClassificacaoService {

    private static final Logger log = LoggerFactory.getLogger(ClassificacaoService.class);

    public record LinhaDaTabela(int posicao, UUID timeId, String nome, String tag, String logoUrl, int jogadas, int vitorias,
                                int derrotas, int jogosAFavor, int jogosContra, int saldo, int pontos, boolean empate) {
    }

    public record EmpateAberto(int posicao, List<UUID> times) {
    }

    public record Tabela(UUID faseId, UUID campeonatoId, int melhorDe, List<LinhaDaTabela> linhas,
                         List<EmpateAberto> empatesAbertos, int decisoesOrfas, Instant atualizadoEm) {
    }

    public record Recalculo(boolean consistente, Tabela tabela) {
    }

    private final FaseRepositorio fases;
    private final ParticipacaoRepositorio participacoes;
    private final PartidaRepositorio partidas;
    private final TimeRepositorio times;
    private final DecisaoDesempateRepositorio decisoes;
    private final CampeonatoService campeonatos;
    private final UsuarioRepositorio usuarios;
    private final AuditoriaService auditoria;
    private final ImageStorage armazenamento;

    public ClassificacaoService(FaseRepositorio fases, ParticipacaoRepositorio participacoes, PartidaRepositorio partidas,
                                TimeRepositorio times, DecisaoDesempateRepositorio decisoes, CampeonatoService campeonatos,
                                UsuarioRepositorio usuarios, AuditoriaService auditoria, ImageStorage armazenamento) {
        this.armazenamento = armazenamento;
        this.fases = fases;
        this.participacoes = participacoes;
        this.partidas = partidas;
        this.times = times;
        this.decisoes = decisoes;
        this.campeonatos = campeonatos;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Tabela tabela(UUID faseId) {
        FaseEntidade fase = fase(faseId);
        return montar(fase, calcular(fase));
    }

    /** Posição manual para um empate que nenhum critério resolve; vale só para o conjunto exato de empatados. */
    @Transactional
    public Tabela decidirEmpate(String autor, UUID faseId, List<UUID> ordem, String motivo) {
        FaseEntidade fase = fase(faseId);
        DecisaoManual decisao;
        try {
            decisao = new DecisaoManual(ordem == null ? List.of() : ordem, motivo);
        } catch (IllegalArgumentException e) {
            throw ErroDeRegra.regra("DECISAO_INVALIDA", e.getMessage());
        }
        // Só aceita se o conjunto for exatamente um empate que existe agora (sem as decisões já guardadas).
        Classificacao semDecisoes = Standings.compute(participantes(fase), resultados(fase),
                campeonatos.regras(fase.getCampeonatoId()), List.of());
        boolean empateExiste = empates(semDecisoes.linhas()).stream()
                .anyMatch(e -> Set.copyOf(e.times()).equals(decisao.conjunto()));
        if (!empateExiste) {
            throw ErroDeRegra.regra("EMPATE_INEXISTENTE",
                    "Nenhum empate atual envolve exatamente estes Times; a decisão não foi gravada.");
        }
        DecisaoDesempateEntidade e = new DecisaoDesempateEntidade();
        e.setFaseId(faseId);
        e.setTimesIds(decisao.ordem().stream().sorted().toArray(UUID[]::new));
        e.setOrdemIds(decisao.ordem().toArray(UUID[]::new));
        e.setMotivo(decisao.motivo().strip());
        e.setAdminId(usuarios.buscarPorEmail(autor).map(UsuarioEntidade::getId).orElse(null));
        decisoes.save(e);
        auditoria.registrar(autor, "fase", faseId, "DECIDIR_EMPATE", null,
                Map.of("ordem", decisao.ordem(), "motivo", e.getMotivo()));
        return montar(fase, calcular(fase));
    }

    /**
     * Recálculo sob demanda (FR-14): refaz a tabela do zero e compara com a consulta normal. Qualquer
     * diferença indica defeito, e não rotina, e fica no log como erro de consistência.
     */
    @Transactional(readOnly = true)
    public Recalculo recalcular(UUID faseId) {
        FaseEntidade fase = fase(faseId);
        Classificacao consulta = calcular(fase);
        Classificacao recalculo = Standings.compute(participantes(fase), resultados(fase),
                campeonatos.regras(fase.getCampeonatoId()), decisoesManuais(fase));
        boolean consistente = consulta.equals(recalculo);
        if (!consistente) {
            log.error("Erro de consistência na classificação da fase {}: o recálculo difere da consulta.", faseId);
        }
        return new Recalculo(consistente, montar(fase, recalculo));
    }

    private Classificacao calcular(FaseEntidade fase) {
        return Standings.compute(participantes(fase), resultados(fase),
                campeonatos.regras(fase.getCampeonatoId()), decisoesManuais(fase));
    }

    private List<UUID> participantes(FaseEntidade fase) {
        return participacoes.findByCampeonatoId(fase.getCampeonatoId()).stream()
                .map(ParticipacaoEntidade::getTimeId).toList();
    }

    private List<PartidaResultado> resultados(FaseEntidade fase) {
        List<PartidaResultado> lista = new ArrayList<>();
        for (PartidaEntidade p : partidas.findByFaseIdOrderByRodadaAscPosicaoAsc(fase.getId())) {
            EstadoPartida estado = EstadoPartida.valueOf(p.getEstado());
            if (estado == EstadoPartida.FINALIZADA && p.getTimeAId() != null && p.getTimeBId() != null) {
                lista.add(new PartidaResultado(p.getTimeAId(), p.getTimeBId(), p.getPlacarA(), p.getPlacarB(),
                        p.isWalkover(), estado, p.getRodada()));
            }
        }
        return lista;
    }

    private List<DecisaoManual> decisoesManuais(FaseEntidade fase) {
        return decisoes.findByFaseIdOrderByCriadoEmAsc(fase.getId()).stream()
                .map(d -> new DecisaoManual(Arrays.asList(d.getOrdemIds()), d.getMotivo())).toList();
    }

    private Tabela montar(FaseEntidade fase, Classificacao c) {
        Map<UUID, TimeEntidade> porId = new HashMap<>();
        times.findAllById(c.linhas().stream().map(Linha::timeId).toList()).forEach(t -> porId.put(t.getId(), t));
        List<LinhaDaTabela> linhas = c.linhas().stream().map(l -> {
            TimeEntidade t = porId.get(l.timeId());
            return new LinhaDaTabela(l.posicao(), l.timeId(), t == null ? "" : t.getNome(), t == null ? "" : t.getTag(),
                    t == null ? null : armazenamento.publicUrl(t.getLogoUrl()),
                    l.jogadas(), l.vitorias(), l.derrotas(), l.jogosAFavor(), l.jogosContra(), l.saldo(), l.pontos(),
                    l.empate());
        }).toList();
        return new Tabela(fase.getId(), fase.getCampeonatoId(), campeonatos.regras(fase.getCampeonatoId()).melhorDe(),
                linhas, empates(c.linhas()), c.decisoesOrfas().size(), Instant.now());
    }

    /** Grupos de Times que dividem a mesma posição porque nada os separou. */
    private static List<EmpateAberto> empates(List<Linha> linhas) {
        Map<Integer, List<UUID>> porPosicao = new LinkedHashMap<>();
        for (Linha l : linhas) {
            if (l.empate()) {
                porPosicao.computeIfAbsent(l.posicao(), p -> new ArrayList<>()).add(l.timeId());
            }
        }
        return porPosicao.entrySet().stream().map(e -> new EmpateAberto(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    private FaseEntidade fase(UUID faseId) {
        return fases.findById(faseId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("FASE_NAO_ENCONTRADA", "Fase não encontrada."));
    }
}
