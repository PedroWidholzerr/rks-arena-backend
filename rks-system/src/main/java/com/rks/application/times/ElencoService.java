package com.rks.application.times;

import com.rks.application.auditoria.AuditoriaService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.CampeonatoJogadorRepositorio;
import com.rks.infra.persistencia.CampeonatoRepositorio;
import com.rks.infra.persistencia.ElencoMembroRepositorio;
import com.rks.infra.persistencia.ElencoRepositorio;
import com.rks.infra.persistencia.FaseRepositorio;
import com.rks.infra.persistencia.JogadorRepositorio;
import com.rks.infra.persistencia.ParticipacaoRepositorio;
import com.rks.infra.persistencia.PartidaRepositorio;
import com.rks.infra.persistencia.TemporadaRepositorio;
import com.rks.infra.persistencia.TimeRepositorio;
import com.rks.infra.persistencia.entidade.CampeonatoEntidade;
import com.rks.infra.persistencia.entidade.CampeonatoJogadorEntidade;
import com.rks.infra.persistencia.entidade.ElencoEntidade;
import com.rks.infra.persistencia.entidade.ElencoMembroEntidade;
import com.rks.infra.persistencia.entidade.JogadorEntidade;
import com.rks.infra.persistencia.entidade.ParticipacaoEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Elenco da Temporada e participação no Campeonato (FR-6, FR-31 base, AD-11). Na O1 o Tamanho de
 * time é fixo: 3 titulares e até 2 reservas, com exatamente um Capitão. O Elenco é salvo por um
 * comando agregado: o estado transitório sem Capitão nunca é persistido. Um Jogador só entra em
 * um Elenco por Campeonato ({@code campeonato_jogador}, gravada na mesma transação).
 */
@Service
public class ElencoService {

    public static final int TITULARES = 3;
    public static final int RESERVAS_MAXIMAS = 2;

    public record MembroPedido(UUID jogadorId, String funcao, boolean capitao) {
    }

    public record Membro(UUID jogadorId, String nick, String tag, String funcao, boolean capitao) {
    }

    public record Elenco(UUID id, UUID timeId, UUID temporadaId, List<Membro> membros) {
    }

    public record Participacao(UUID id, UUID campeonatoId, UUID timeId, String timeNome, String timeTag, UUID elencoId) {
    }

    private final ElencoRepositorio elencos;
    private final ElencoMembroRepositorio membros;
    private final JogadorRepositorio jogadores;
    private final TimeRepositorio times;
    private final TemporadaRepositorio temporadas;
    private final CampeonatoRepositorio campeonatos;
    private final ParticipacaoRepositorio participacoes;
    private final CampeonatoJogadorRepositorio campeonatoJogadores;
    private final FaseRepositorio fases;
    private final PartidaRepositorio partidas;
    private final AuditoriaService auditoria;

    public ElencoService(ElencoRepositorio elencos, ElencoMembroRepositorio membros, JogadorRepositorio jogadores,
                         TimeRepositorio times, TemporadaRepositorio temporadas, CampeonatoRepositorio campeonatos,
                         ParticipacaoRepositorio participacoes, CampeonatoJogadorRepositorio campeonatoJogadores,
                         FaseRepositorio fases, PartidaRepositorio partidas, AuditoriaService auditoria) {
        this.elencos = elencos;
        this.membros = membros;
        this.jogadores = jogadores;
        this.times = times;
        this.temporadas = temporadas;
        this.campeonatos = campeonatos;
        this.participacoes = participacoes;
        this.campeonatoJogadores = campeonatoJogadores;
        this.fases = fases;
        this.partidas = partidas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Elenco buscar(UUID timeId, UUID temporadaId) {
        ElencoEntidade e = elencos.findByTimeIdAndTemporadaId(timeId, temporadaId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("ELENCO_NAO_ENCONTRADO", "O Time não tem Elenco nesta Temporada."));
        return paraDto(e);
    }

    @Transactional(readOnly = true)
    public List<Elenco> doTime(UUID timeId) {
        return elencos.findByTimeIdOrderByTemporadaId(timeId).stream().map(this::paraDto).toList();
    }

    /** Valida a regra do Elenco e grava tudo ou nada. */
    @Transactional
    public Elenco salvar(String autor, UUID timeId, UUID temporadaId, List<MembroPedido> pedido) {
        if (!times.existsById(timeId)) {
            throw ErroDeRegra.naoEncontrado("TIME_NAO_ENCONTRADO", "Time não encontrado.");
        }
        if (!temporadas.existsById(temporadaId)) {
            throw ErroDeRegra.naoEncontrado("TEMPORADA_NAO_ENCONTRADA", "Temporada não encontrada.");
        }
        validar(pedido);

        ElencoEntidade elenco = elencos.findByTimeIdAndTemporadaId(timeId, temporadaId).orElseGet(() -> {
            ElencoEntidade novo = new ElencoEntidade();
            novo.setTimeId(timeId);
            novo.setTemporadaId(temporadaId);
            return elencos.save(novo);
        });
        Set<UUID> anteriores = membros.findByElencoId(elenco.getId()).stream()
                .map(ElencoMembroEntidade::getJogadorId).collect(Collectors.toSet());
        Set<UUID> novos = pedido.stream().map(MembroPedido::jogadorId).collect(Collectors.toSet());

        // Cada Campeonato em que o Time disputa com este Elenco precisa continuar com um Jogador por Elenco.
        List<ParticipacaoEntidade> disputas = participacoes.findByElencoId(elenco.getId());
        for (ParticipacaoEntidade disputa : disputas) {
            exigirSemConflito(disputa.getCampeonatoId(), anteriores, novos);
        }

        membros.apagarDoElenco(elenco.getId());
        List<ElencoMembroEntidade> gravados = new ArrayList<>();
        for (MembroPedido m : pedido) {
            gravados.add(new ElencoMembroEntidade(elenco.getId(), m.jogadorId(), m.funcao(), m.capitao()));
        }
        membros.saveAll(gravados);

        for (ParticipacaoEntidade disputa : disputas) {
            sincronizar(disputa.getCampeonatoId(), anteriores, novos);
        }
        auditoria.registrar(autor, "elenco", elenco.getId(), "SALVAR",
                Map.of("jogadores", anteriores), Map.of("jogadores", novos));
        return paraDto(elenco);
    }

    @Transactional(readOnly = true)
    public List<Participacao> participacoesDoCampeonato(UUID campeonatoId) {
        return participacoes.findByCampeonatoId(campeonatoId).stream().map(this::paraParticipacao).toList();
    }

    /** Vincula o Time ao Campeonato usando o Elenco da Temporada do Campeonato. */
    @Transactional
    public Participacao vincular(String autor, UUID campeonatoId, UUID timeId) {
        CampeonatoEntidade campeonato = campeonatos.findById(campeonatoId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("CAMPEONATO_NAO_ENCONTRADO", "Campeonato não encontrado."));
        if (!times.existsById(timeId)) {
            throw ErroDeRegra.naoEncontrado("TIME_NAO_ENCONTRADO", "Time não encontrado.");
        }
        if (participacoes.findByCampeonatoIdAndTimeId(campeonatoId, timeId).isPresent()) {
            throw ErroDeRegra.conflito("TIME_JA_VINCULADO", "O Time já participa deste Campeonato.");
        }
        ElencoEntidade elenco = elencos.findByTimeIdAndTemporadaId(timeId, campeonato.getTemporadaId())
                .orElseThrow(() -> ErroDeRegra.regra("ELENCO_AUSENTE",
                        "Monte o Elenco do Time nesta Temporada antes de vinculá-lo ao Campeonato."));
        if (campeonato.getLimiteTimes() != null
                && participacoes.findByCampeonatoId(campeonatoId).size() >= campeonato.getLimiteTimes()) {
            throw ErroDeRegra.regra("LIMITE_DE_TIMES", "O Campeonato já atingiu o limite de Times.");
        }
        Set<UUID> doElenco = membros.findByElencoId(elenco.getId()).stream()
                .map(ElencoMembroEntidade::getJogadorId).collect(Collectors.toSet());
        exigirSemConflito(campeonatoId, Set.of(), doElenco);
        sincronizar(campeonatoId, Set.of(), doElenco);

        ParticipacaoEntidade p = new ParticipacaoEntidade();
        p.setCampeonatoId(campeonatoId);
        p.setTimeId(timeId);
        p.setElencoId(elenco.getId());
        p = participacoes.save(p);
        auditoria.registrar(autor, "participacao", p.getId(), "VINCULAR", null,
                Map.of("campeonatoId", campeonatoId, "timeId", timeId));
        return paraParticipacao(p);
    }

    /** Remove o Time do Campeonato, desde que ele ainda não tenha Partidas lá. */
    @Transactional
    public void desvincular(String autor, UUID campeonatoId, UUID timeId) {
        ParticipacaoEntidade p = participacoes.findByCampeonatoIdAndTimeId(campeonatoId, timeId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("PARTICIPACAO_NAO_ENCONTRADA", "O Time não participa deste Campeonato."));
        boolean temPartidas = fases.findByCampeonatoIdOrderByOrdem(campeonatoId).stream()
                .flatMap(f -> partidas.findByFaseIdOrderByRodadaAscPosicaoAsc(f.getId()).stream())
                .anyMatch(m -> timeId.equals(m.getTimeAId()) || timeId.equals(m.getTimeBId()));
        if (temPartidas) {
            throw ErroDeRegra.conflito("TIME_COM_PARTIDAS", "O Time já tem Partidas neste Campeonato; exclua-as antes.");
        }
        Set<UUID> doElenco = membros.findByElencoId(p.getElencoId()).stream()
                .map(ElencoMembroEntidade::getJogadorId).collect(Collectors.toSet());
        if (!doElenco.isEmpty()) {
            campeonatoJogadores.apagar(campeonatoId, doElenco);
        }
        participacoes.delete(p);
        auditoria.registrar(autor, "participacao", p.getId(), "DESVINCULAR",
                Map.of("campeonatoId", campeonatoId, "timeId", timeId), null);
    }

    /** Regra do Elenco da O1: 3 titulares, até 2 reservas, exatamente um Capitão, sem Jogador repetido. */
    void validar(List<MembroPedido> pedido) {
        if (pedido == null || pedido.isEmpty()) {
            throw ErroDeRegra.regra("ELENCO_INVALIDO", "Informe os Jogadores do Elenco.");
        }
        Set<UUID> vistos = new HashSet<>();
        int titulares = 0;
        int reservas = 0;
        int capitaes = 0;
        for (MembroPedido m : pedido) {
            if (m == null || m.jogadorId() == null || !vistos.add(m.jogadorId())) {
                throw ErroDeRegra.regra("ELENCO_INVALIDO", "Cada Jogador entra uma única vez no Elenco.");
            }
            if ("TITULAR".equals(m.funcao())) {
                titulares++;
            } else if ("RESERVA".equals(m.funcao())) {
                reservas++;
            } else {
                throw ErroDeRegra.regra("FUNCAO_INVALIDA", "A função deve ser TITULAR ou RESERVA.");
            }
            if (m.capitao()) {
                capitaes++;
            }
        }
        if (titulares != TITULARES || reservas > RESERVAS_MAXIMAS) {
            throw ErroDeRegra.regra("TAMANHO_DE_ELENCO_INVALIDO",
                    "O Elenco precisa de " + TITULARES + " titulares e no máximo " + RESERVAS_MAXIMAS + " reservas.");
        }
        if (capitaes != 1) {
            throw ErroDeRegra.regra("CAPITAO_OBRIGATORIO", "O Elenco precisa de exatamente um Capitão.");
        }
        for (UUID id : vistos) {
            if (!jogadores.existsById(id)) {
                throw ErroDeRegra.naoEncontrado("JOGADOR_NAO_ENCONTRADO", "Jogador não encontrado: " + id + ".");
            }
        }
    }

    /** Um Jogador só pode estar em um Elenco por Campeonato: confere os que entram contra os de outros Times. */
    private void exigirSemConflito(UUID campeonatoId, Set<UUID> doMesmoElenco, Set<UUID> entrando) {
        Set<UUID> deOutros = campeonatoJogadores.findByCampeonatoId(campeonatoId).stream()
                .map(CampeonatoJogadorEntidade::getJogadorId)
                .filter(id -> !doMesmoElenco.contains(id))
                .collect(Collectors.toSet());
        for (UUID id : entrando) {
            if (deOutros.contains(id)) {
                String nome = jogadores.findById(id).map(JogadorEntidade::getNick).orElse(id.toString());
                throw ErroDeRegra.conflito("JOGADOR_EM_OUTRO_ELENCO",
                        "O Jogador " + nome + " já está no Elenco de outro Time neste Campeonato.",
                        Map.of("jogadorId", id));
            }
        }
    }

    private void sincronizar(UUID campeonatoId, Set<UUID> anteriores, Set<UUID> novos) {
        Set<UUID> sairam = new HashSet<>(anteriores);
        sairam.removeAll(novos);
        if (!sairam.isEmpty()) {
            campeonatoJogadores.apagar(campeonatoId, sairam);
        }
        for (UUID id : novos) {
            if (!anteriores.contains(id)) {
                campeonatoJogadores.save(new CampeonatoJogadorEntidade(campeonatoId, id));
            }
        }
    }

    private Elenco paraDto(ElencoEntidade e) {
        List<Membro> lista = membros.findByElencoId(e.getId()).stream()
                .map(m -> {
                    JogadorEntidade j = jogadores.findById(m.getJogadorId()).orElseThrow();
                    return new Membro(j.getId(), j.getNick(), j.getTag(), m.getFuncao(), m.isCapitao());
                })
                .sorted(java.util.Comparator.comparing((Membro m) -> !m.capitao())
                        .thenComparing(m -> !"TITULAR".equals(m.funcao()))
                        .thenComparing(m -> m.nick().toLowerCase()))
                .toList();
        return new Elenco(e.getId(), e.getTimeId(), e.getTemporadaId(), lista);
    }

    private Participacao paraParticipacao(ParticipacaoEntidade p) {
        var time = times.findById(p.getTimeId()).orElseThrow();
        return new Participacao(p.getId(), p.getCampeonatoId(), p.getTimeId(), time.getNome(), time.getTag(), p.getElencoId());
    }
}
