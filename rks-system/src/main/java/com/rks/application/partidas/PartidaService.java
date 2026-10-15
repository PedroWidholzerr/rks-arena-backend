package com.rks.application.partidas;

import com.rks.application.auditoria.AuditoriaService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.domain.rules.EstadoPartida;
import com.rks.domain.rules.MaquinaDePartida;
import com.rks.domain.rules.MaquinaDePartida.Placar;
import com.rks.domain.rules.MaquinaDePartida.Situacao;
import com.rks.infra.persistencia.CampeonatoRepositorio;
import com.rks.infra.persistencia.ElencoMembroRepositorio;
import com.rks.infra.persistencia.FaseRepositorio;
import com.rks.infra.persistencia.ParticipacaoRepositorio;
import com.rks.infra.persistencia.PartidaRepositorio;
import com.rks.infra.persistencia.TimeRepositorio;
import com.rks.infra.persistencia.entidade.CampeonatoEntidade;
import com.rks.infra.persistencia.entidade.ElencoMembroEntidade;
import com.rks.infra.persistencia.entidade.FaseEntidade;
import com.rks.infra.persistencia.entidade.ParticipacaoEntidade;
import com.rks.infra.persistencia.entidade.PartidaEntidade;
import com.rks.infra.persistencia.entidade.TimeEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Partidas do Campeonato (FR-7 a FR-10, FR-36 campo): agendar, iniciar, lançar resultado, W.O.,
 * editar e anular. Toda escrita de resultado trava o Campeonato com {@code FOR UPDATE}, a mesma
 * trava da edição de configuração (AD-10), confere a {@code version} (409 se defasada) e grava
 * auditoria na mesma transação. O placar só é validado por {@link MaquinaDePartida} (AD-3, AD-6).
 */
@Service
public class PartidaService {

    public record TimeResumo(UUID id, String nome, String tag) {
    }

    public record Partida(UUID id, UUID faseId, UUID campeonatoId, int rodada, int posicao, TimeResumo timeA,
                          TimeResumo timeB, String estado, Integer jogosA, Integer jogosB, boolean walkover,
                          Instant agendadaPara, String streamUrl, UUID mvpJogadorId, long version,
                          Instant atualizadoEm) {
    }

    private final PartidaRepositorio partidas;
    private final FaseRepositorio fases;
    private final CampeonatoRepositorio campeonatos;
    private final ParticipacaoRepositorio participacoes;
    private final ElencoMembroRepositorio elencoMembros;
    private final TimeRepositorio times;
    private final AuditoriaService auditoria;

    public PartidaService(PartidaRepositorio partidas, FaseRepositorio fases, CampeonatoRepositorio campeonatos,
                          ParticipacaoRepositorio participacoes, ElencoMembroRepositorio elencoMembros,
                          TimeRepositorio times, AuditoriaService auditoria) {
        this.partidas = partidas;
        this.fases = fases;
        this.campeonatos = campeonatos;
        this.participacoes = participacoes;
        this.elencoMembros = elencoMembros;
        this.times = times;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Partida> listarDaFase(UUID faseId) {
        FaseEntidade fase = fase(faseId);
        return partidas.findByFaseIdOrderByRodadaAscPosicaoAsc(faseId).stream()
                .map(p -> paraDto(p, fase.getCampeonatoId())).toList();
    }

    /** Partidas do Campeonato, com filtros opcionais por Fase, Time e dia (fuso America/Sao_Paulo). */
    @Transactional(readOnly = true)
    public List<Partida> listarDoCampeonato(UUID campeonatoId, UUID faseId, UUID timeId, java.time.LocalDate dia) {
        java.time.ZoneId fuso = java.time.ZoneId.of("America/Sao_Paulo");
        List<Partida> lista = new java.util.ArrayList<>();
        for (FaseEntidade fase : fases.findByCampeonatoIdOrderByOrdem(campeonatoId)) {
            if (faseId != null && !faseId.equals(fase.getId())) {
                continue;
            }
            for (PartidaEntidade p : partidas.findByFaseIdOrderByRodadaAscPosicaoAsc(fase.getId())) {
                boolean doTime = timeId == null || timeId.equals(p.getTimeAId()) || timeId.equals(p.getTimeBId());
                boolean doDia = dia == null || (p.getAgendadaPara() != null && p.getAgendadaPara().atZone(fuso).toLocalDate().equals(dia));
                if (doTime && doDia) {
                    lista.add(paraDto(p, campeonatoId));
                }
            }
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public Partida buscar(UUID id) {
        PartidaEntidade p = obter(id);
        return paraDto(p, fase(p.getFaseId()).getCampeonatoId());
    }

    @Transactional
    public Partida agendar(String autor, UUID faseId, UUID timeAId, UUID timeBId, Integer rodada,
                           Instant agendadaPara, String streamUrl) {
        FaseEntidade fase = fase(faseId);
        validarTimes(fase.getCampeonatoId(), timeAId, timeBId);
        PartidaEntidade p = new PartidaEntidade();
        p.setFaseId(faseId);
        p.setRodada((short) (rodada == null ? 1 : Math.max(1, rodada)));
        p.setPosicao((short) (proximaPosicao(faseId, p.getRodada())));
        p.setTimeAId(timeAId);
        p.setTimeBId(timeBId);
        p.setAgendadaPara(agendadaPara);
        p.setStreamUrl(validarStream(streamUrl));
        p = partidas.save(p);
        auditoria.registrar(autor, "partida", p.getId(), "AGENDAR", null, resumo(p));
        return paraDto(p, fase.getCampeonatoId());
    }

    /** Reagendar e trocar o link; os Times só mudam enquanto a Partida está AGENDADA. */
    @Transactional
    public Partida atualizar(String autor, UUID id, long versao, UUID timeAId, UUID timeBId, Instant agendadaPara,
                             String streamUrl) {
        PartidaEntidade p = obter(id);
        exigirVersao(p, versao);
        if (EstadoPartida.valueOf(p.getEstado()) == EstadoPartida.FINALIZADA) {
            throw ErroDeRegra.conflito("PARTIDA_FINALIZADA", "A Partida está finalizada; anule o resultado para alterá-la.");
        }
        UUID campeonatoId = fase(p.getFaseId()).getCampeonatoId();
        Map<String, Object> antes = resumo(p);
        boolean mudouTimes = !java.util.Objects.equals(timeAId, p.getTimeAId()) || !java.util.Objects.equals(timeBId, p.getTimeBId());
        if (mudouTimes) {
            if (!EstadoPartida.AGENDADA.name().equals(p.getEstado())) {
                throw ErroDeRegra.conflito("PARTIDA_EM_ANDAMENTO", "Os Times só mudam enquanto a Partida está Agendada.");
            }
            validarTimes(campeonatoId, timeAId, timeBId);
            p.setTimeAId(timeAId);
            p.setTimeBId(timeBId);
        }
        p.setAgendadaPara(agendadaPara);
        p.setStreamUrl(validarStream(streamUrl));
        p.setAtualizadoEm(Instant.now());
        p = partidas.save(p);
        auditoria.registrar(autor, "partida", id, "ATUALIZAR", antes, resumo(p));
        return paraDto(p, campeonatoId);
    }

    @Transactional
    public void excluir(String autor, UUID id) {
        PartidaEntidade p = obter(id);
        if (EstadoPartida.valueOf(p.getEstado()) == EstadoPartida.FINALIZADA) {
            throw ErroDeRegra.conflito("PARTIDA_FINALIZADA", "A Partida está finalizada; anule o resultado antes de excluí-la.");
        }
        partidas.delete(p);
        auditoria.registrar(autor, "partida", id, "EXCLUIR", resumo(p), null);
    }

    @Transactional
    public Partida iniciar(String autor, UUID id, long versao) {
        return transicionar(autor, id, versao, "INICIAR", (p, campeonato) -> MaquinaDePartida.iniciar(situacao(p)), null);
    }

    /** Lança o resultado; em Partida já FINALIZADA, é a edição do resultado (mesma validação). */
    @Transactional
    public Partida lancarResultado(String autor, UUID id, long versao, int jogosA, int jogosB, UUID mvpJogadorId) {
        return transicionar(autor, id, versao, "RESULTADO", (p, campeonato) -> {
            Situacao atual = situacao(p);
            int melhorDe = campeonato.getMelhorDe();
            return atual.estado() == EstadoPartida.FINALIZADA
                    ? MaquinaDePartida.editarResultado(atual, melhorDe, jogosA, jogosB)
                    : MaquinaDePartida.lancarResultado(atual, melhorDe, jogosA, jogosB);
        }, mvpJogadorId);
    }

    @Transactional
    public Partida registrarWalkover(String autor, UUID id, long versao, boolean vencedorEhA) {
        return transicionar(autor, id, versao, "WALKOVER",
                (p, campeonato) -> MaquinaDePartida.registrarWalkover(situacao(p), campeonato.getMelhorDe(), vencedorEhA), null);
    }

    /** Anular resultado: FINALIZADA volta a AGENDADA, sem placar e sem MVP. Cancelar (ANULADA) fica para a O3. */
    @Transactional
    public Partida anularResultado(String autor, UUID id, long versao) {
        return transicionar(autor, id, versao, "ANULAR_RESULTADO",
                (p, campeonato) -> MaquinaDePartida.anularResultado(situacao(p)), null);
    }

    @FunctionalInterface
    private interface Transicao {
        Situacao aplicar(PartidaEntidade partida, CampeonatoEntidade campeonato);
    }

    private Partida transicionar(String autor, UUID id, long versao, String acao, Transicao transicao, UUID mvp) {
        PartidaEntidade p = obter(id);
        FaseEntidade fase = fase(p.getFaseId());
        // A mesma trava de editar a configuração: um dos dois espera pelo outro (AD-10).
        CampeonatoEntidade campeonato = campeonatos.buscarParaAtualizar(fase.getCampeonatoId())
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("CAMPEONATO_NAO_ENCONTRADO", "Campeonato não encontrado."));
        exigirVersao(p, versao);
        if (p.getTimeAId() == null || p.getTimeBId() == null) {
            throw ErroDeRegra.regra("PARTIDA_SEM_TIMES", "A Partida ainda não tem os dois Times definidos.");
        }
        Map<String, Object> antes = resumo(p);
        Situacao nova = transicao.aplicar(p, campeonato);
        if (mvp != null) {
            validarMvp(campeonato, p, nova, mvp);
        }
        aplicar(p, nova, nova.estado() == EstadoPartida.FINALIZADA && !nova.placar().walkover() ? mvp : null);
        p.setAtualizadoEm(Instant.now());
        p = partidas.saveAndFlush(p);
        auditoria.registrar(autor, "partida", id, acao, antes, resumo(p));
        return paraDto(p, campeonato.getId());
    }

    private void validarMvp(CampeonatoEntidade campeonato, PartidaEntidade p, Situacao nova, UUID mvp) {
        if (nova.estado() != EstadoPartida.FINALIZADA || nova.placar().walkover()) {
            throw ErroDeRegra.regra("MVP_INVALIDO", "O MVP só vale em resultado lançado, e não em W.O.");
        }
        boolean doElenco = false;
        for (UUID timeId : List.of(p.getTimeAId(), p.getTimeBId())) {
            Optional<ParticipacaoEntidade> participacao = participacoes.findByCampeonatoIdAndTimeId(campeonato.getId(), timeId);
            if (participacao.isPresent()) {
                doElenco |= elencoMembros.findByElencoId(participacao.get().getElencoId()).stream()
                        .map(ElencoMembroEntidade::getJogadorId).anyMatch(mvp::equals);
            }
        }
        if (!doElenco) {
            throw ErroDeRegra.regra("MVP_INVALIDO", "O MVP precisa ser do Elenco de um dos dois Times da Partida.");
        }
    }

    private static void aplicar(PartidaEntidade p, Situacao nova, UUID mvp) {
        p.setEstado(nova.estado().name());
        Placar placar = nova.placar();
        p.setPlacarA(placar == null ? null : (short) placar.jogosA());
        p.setPlacarB(placar == null ? null : (short) placar.jogosB());
        p.setWalkover(placar != null && placar.walkover());
        p.setMvpJogadorId(mvp);
    }

    private static Situacao situacao(PartidaEntidade p) {
        EstadoPartida estado = EstadoPartida.valueOf(p.getEstado());
        if (estado == EstadoPartida.FINALIZADA) {
            return new Situacao(estado, new Placar(p.getPlacarA(), p.getPlacarB(), p.isWalkover()));
        }
        if (estado == EstadoPartida.ANULADA) {
            throw ErroDeRegra.conflito("PARTIDA_ANULADA", "A Partida está cancelada.");
        }
        return new Situacao(estado, null);
    }

    private static void exigirVersao(PartidaEntidade p, long versao) {
        if (p.getVersion() != versao) {
            throw ErroDeRegra.conflito("VERSAO_DESATUALIZADA",
                    "Esta Partida mudou desde que você a abriu. Recarregue para ver o estado atual.",
                    Map.of("versaoAtual", p.getVersion()));
        }
    }

    private void validarTimes(UUID campeonatoId, UUID timeAId, UUID timeBId) {
        if (timeAId == null || timeBId == null) {
            throw ErroDeRegra.regra("TIMES_OBRIGATORIOS", "Informe os dois Times da Partida.");
        }
        if (timeAId.equals(timeBId)) {
            throw ErroDeRegra.regra("TIMES_IGUAIS", "Um Time não enfrenta a si mesmo.");
        }
        for (UUID timeId : List.of(timeAId, timeBId)) {
            if (participacoes.findByCampeonatoIdAndTimeId(campeonatoId, timeId).isEmpty()) {
                throw ErroDeRegra.regra("TIME_FORA_DO_CAMPEONATO", "Os dois Times precisam participar do Campeonato.");
            }
        }
    }

    /** Só https é aceito (FR-36). */
    static String validarStream(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.strip());
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && url.length() <= 500) {
                return url.strip();
            }
        } catch (IllegalArgumentException ignorado) {
            // cai no erro abaixo
        }
        throw ErroDeRegra.regra("STREAM_URL_INVALIDA", "O link de transmissão precisa ser uma URL https.");
    }

    private int proximaPosicao(UUID faseId, short rodada) {
        return partidas.findByFaseIdOrderByRodadaAscPosicaoAsc(faseId).stream()
                .filter(m -> m.getRodada() == rodada).mapToInt(PartidaEntidade::getPosicao).max().orElse(0) + 1;
    }

    private PartidaEntidade obter(UUID id) {
        return partidas.findById(id)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("PARTIDA_NAO_ENCONTRADA", "Partida não encontrada."));
    }

    private FaseEntidade fase(UUID faseId) {
        return fases.findById(faseId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("FASE_NAO_ENCONTRADA", "Fase não encontrada."));
    }

    private Partida paraDto(PartidaEntidade p, UUID campeonatoId) {
        return new Partida(p.getId(), p.getFaseId(), campeonatoId, p.getRodada(), p.getPosicao(),
                resumoDoTime(p.getTimeAId()), resumoDoTime(p.getTimeBId()), p.getEstado(),
                p.getPlacarA() == null ? null : p.getPlacarA().intValue(),
                p.getPlacarB() == null ? null : p.getPlacarB().intValue(), p.isWalkover(), p.getAgendadaPara(),
                p.getStreamUrl(), p.getMvpJogadorId(), p.getVersion(), p.getAtualizadoEm());
    }

    private TimeResumo resumoDoTime(UUID id) {
        if (id == null) {
            return null;
        }
        TimeEntidade t = times.findById(id).orElse(null);
        return t == null ? null : new TimeResumo(t.getId(), t.getNome(), t.getTag());
    }

    private static Map<String, Object> resumo(PartidaEntidade p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("estado", p.getEstado());
        m.put("placar", p.getPlacarA() == null ? null : p.getPlacarA() + "x" + p.getPlacarB());
        m.put("walkover", p.isWalkover());
        m.put("timeA", p.getTimeAId());
        m.put("timeB", p.getTimeBId());
        m.put("agendadaPara", p.getAgendadaPara() == null ? null : p.getAgendadaPara().toString());
        m.put("mvp", p.getMvpJogadorId());
        return m;
    }
}
