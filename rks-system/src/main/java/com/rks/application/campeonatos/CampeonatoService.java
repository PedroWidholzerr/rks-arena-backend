package com.rks.application.campeonatos;

import com.rks.application.auditoria.AuditoriaService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.domain.rules.Pontuacao;
import com.rks.domain.standings.CriterioDesempate;
import com.rks.domain.standings.RegrasClassificacao;
import com.rks.infra.persistencia.CampeonatoRepositorio;
import com.rks.infra.persistencia.FaseRepositorio;
import com.rks.infra.persistencia.TemporadaRepositorio;
import com.rks.infra.persistencia.entidade.CampeonatoEntidade;
import com.rks.infra.persistencia.entidade.FaseEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Campeonatos de pontos corridos (FR-2 parcial, FR-3, FR-11). Criar também cria a Fase implícita.
 * A configuração (melhor de N, pontuação, ordem dos critérios) congela com a primeira Partida
 * FINALIZADA (AD-10); a mesma trava {@code FOR UPDATE} serve a editar a regra e a lançar resultado.
 * Modo e Tamanho de time não são editáveis na O1.
 */
@Service
public class CampeonatoService {

    public static final Set<String> ESTADOS = Set.of("EM_BREVE", "EM_ANDAMENTO", "ENCERRADO");

    public record Config(int melhorDe, Pontuacao pontuacao, List<CriterioDesempate> criterios) {
    }

    public record Campeonato(UUID id, UUID temporadaId, UUID faseId, String nome, String descricao, String modo,
                             int tamanhoTime, Config configuracao, Integer limiteTimes, String estado,
                             boolean congelado, long partidasFinalizadas, Instant criadoEm) {
    }

    private final CampeonatoRepositorio campeonatos;
    private final FaseRepositorio fases;
    private final TemporadaRepositorio temporadas;
    private final AuditoriaService auditoria;

    public CampeonatoService(CampeonatoRepositorio campeonatos, FaseRepositorio fases, TemporadaRepositorio temporadas,
                             AuditoriaService auditoria) {
        this.campeonatos = campeonatos;
        this.fases = fases;
        this.temporadas = temporadas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Campeonato> listarDaTemporada(UUID temporadaId) {
        return campeonatos.findByTemporadaIdOrderByCriadoEmDesc(temporadaId).stream().map(this::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public Campeonato buscar(UUID id) {
        return paraDto(obter(id));
    }

    @Transactional
    public Campeonato criar(String autor, UUID temporadaId, String nome, String descricao, Integer melhorDe,
                            Pontuacao pontuacao, List<CriterioDesempate> criterios) {
        if (!temporadas.existsById(temporadaId)) {
            throw ErroDeRegra.naoEncontrado("TEMPORADA_NAO_ENCONTRADA", "Temporada não encontrada.");
        }
        CampeonatoEntidade c = new CampeonatoEntidade();
        c.setTemporadaId(temporadaId);
        preencherDescricao(c, nome, descricao);
        aplicarConfiguracao(c, melhorDe == null ? 1 : melhorDe,
                pontuacao == null ? Pontuacao.PADRAO : pontuacao,
                criterios == null ? RegrasClassificacao.CRITERIOS_PADRAO : criterios);
        c = campeonatos.save(c);

        FaseEntidade fase = new FaseEntidade();
        fase.setCampeonatoId(c.getId());
        fase.setNome("Pontos corridos");
        fases.save(fase);

        auditoria.registrar(autor, "campeonato", c.getId(), "CRIAR", null, resumo(c));
        return paraDto(c);
    }

    /** Nome, descrição e estado: livres a qualquer momento. */
    @Transactional
    public Campeonato atualizar(String autor, UUID id, String nome, String descricao, String estado) {
        CampeonatoEntidade c = obter(id);
        Map<String, Object> antes = resumo(c);
        preencherDescricao(c, nome, descricao);
        if (estado != null) {
            if (!ESTADOS.contains(estado)) {
                throw ErroDeRegra.regra("ESTADO_INVALIDO", "O estado deve ser EM_BREVE, EM_ANDAMENTO ou ENCERRADO.");
            }
            c.setEstado(estado);
        }
        c = campeonatos.save(c);
        auditoria.registrar(autor, "campeonato", id, "ATUALIZAR", antes, resumo(c));
        return paraDto(c);
    }

    /**
     * Melhor de N, pontuação e ordem dos critérios. Trava a linha do Campeonato e recusa com 409
     * CONFIG_CONGELADA se já houver Partida FINALIZADA; anular as finalizadas libera a edição.
     */
    @Transactional
    public Campeonato atualizarConfiguracao(String autor, UUID id, int melhorDe, Pontuacao pontuacao,
                                            List<CriterioDesempate> criterios) {
        CampeonatoEntidade c = campeonatos.buscarParaAtualizar(id)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("CAMPEONATO_NAO_ENCONTRADO", "Campeonato não encontrado."));
        long finalizadas = campeonatos.contarPartidasFinalizadas(id);
        if (finalizadas > 0) {
            throw ErroDeRegra.conflito("CONFIG_CONGELADA",
                    "A configuração está congelada: o Campeonato tem " + finalizadas
                            + " Partidas finalizadas. Anule os resultados para editar.",
                    Map.of("partidasFinalizadas", finalizadas));
        }
        Map<String, Object> antes = resumo(c);
        aplicarConfiguracao(c, melhorDe, pontuacao, criterios);
        c = campeonatos.save(c);
        auditoria.registrar(autor, "campeonato", id, "CONFIGURAR", antes, resumo(c));
        return paraDto(c);
    }

    public record FaseDto(UUID id, String nome, String formato, int ordem, int turnos) {
    }

    @Transactional(readOnly = true)
    public List<FaseDto> fasesDo(UUID campeonatoId) {
        obter(campeonatoId);
        return fases.findByCampeonatoIdOrderByOrdem(campeonatoId).stream()
                .map(f -> new FaseDto(f.getId(), f.getNome(), f.getFormato(), f.getOrdem(), f.getTurnos())).toList();
    }

    /** Regras para a classificação, a partir do que está gravado. */
    @Transactional(readOnly = true)
    public RegrasClassificacao regras(UUID campeonatoId) {
        return regrasDe(obter(campeonatoId));
    }

    static RegrasClassificacao regrasDe(CampeonatoEntidade c) {
        return new RegrasClassificacao(
                new Pontuacao(c.getPontosVitoria(), c.getPontosDerrotaDecisiva(), c.getPontosDerrota()),
                c.getMelhorDe(), criterios(c.getOrdemDesempate()));
    }

    private CampeonatoEntidade obter(UUID id) {
        return campeonatos.findById(id)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("CAMPEONATO_NAO_ENCONTRADO", "Campeonato não encontrado."));
    }

    private static void preencherDescricao(CampeonatoEntidade c, String nome, String descricao) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty() || limpo.length() > 120) {
            throw ErroDeRegra.regra("NOME_INVALIDO", "O nome do Campeonato é obrigatório e vai até 120 caracteres.");
        }
        c.setNome(limpo);
        c.setDescricao(descricao == null || descricao.isBlank() ? null : descricao.strip());
    }

    private static void aplicarConfiguracao(CampeonatoEntidade c, int melhorDe, Pontuacao pontuacao,
                                            List<CriterioDesempate> criterios) {
        if (melhorDe < 1 || melhorDe % 2 == 0 || melhorDe > 99) {
            throw ErroDeRegra.regra("MELHOR_DE_INVALIDO", "Melhor de N exige N ímpar maior ou igual a 1.");
        }
        try {
            new RegrasClassificacao(pontuacao, melhorDe, criterios);
        } catch (IllegalArgumentException e) {
            throw ErroDeRegra.regra("CONFIGURACAO_INVALIDA", e.getMessage());
        }
        c.setMelhorDe((short) melhorDe);
        c.setPontosVitoria((short) pontuacao.vitoria());
        c.setPontosDerrotaDecisiva((short) pontuacao.derrotaDecisiva());
        c.setPontosDerrota((short) pontuacao.derrota());
        c.setOrdemDesempate(String.join(",", criterios.stream().map(Enum::name).toList()));
    }

    static List<CriterioDesempate> criterios(String texto) {
        List<CriterioDesempate> lista = new ArrayList<>();
        for (String parte : texto.split(",")) {
            lista.add(CriterioDesempate.valueOf(parte.strip()));
        }
        return lista;
    }

    private Campeonato paraDto(CampeonatoEntidade c) {
        long finalizadas = campeonatos.contarPartidasFinalizadas(c.getId());
        UUID faseId = fases.findByCampeonatoIdOrderByOrdem(c.getId()).stream().findFirst()
                .map(FaseEntidade::getId).orElse(null);
        RegrasClassificacao regras = regrasDe(c);
        return new Campeonato(c.getId(), c.getTemporadaId(), faseId, c.getNome(), c.getDescricao(), c.getModo(),
                c.getTamanhoTime(), new Config(regras.melhorDe(), regras.pontuacao(), regras.criterios()),
                c.getLimiteTimes() == null ? null : c.getLimiteTimes().intValue(), c.getEstado(),
                finalizadas > 0, finalizadas, c.getCriadoEm());
    }

    private static Map<String, Object> resumo(CampeonatoEntidade c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", c.getNome());
        m.put("estado", c.getEstado());
        m.put("melhorDe", c.getMelhorDe());
        m.put("pontuacao", List.of(c.getPontosVitoria(), c.getPontosDerrotaDecisiva(), c.getPontosDerrota()));
        m.put("criterios", c.getOrdemDesempate());
        return m;
    }
}
