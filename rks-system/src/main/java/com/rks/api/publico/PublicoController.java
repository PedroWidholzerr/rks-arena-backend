package com.rks.api.publico;

import com.rks.application.campeonatos.CampeonatoService;
import com.rks.application.campeonatos.TemporadaService;
import com.rks.application.classificacao.ClassificacaoService;
import com.rks.application.partidas.PartidaService;
import com.rks.application.times.ElencoService;
import com.rks.application.times.JogadorService;
import com.rks.application.times.TimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Leitura pública, sem login (AD-14): só GET. Todo endpoint de dados devolve {@code atualizadoEm},
 * para a tela dizer "Atualizado há..." (UX-DR14). Nada de contato do Capitão nem de dado interno.
 */
@RestController
@RequestMapping("/api/public")
public class PublicoController {

    /** Envelope das respostas de dados. */
    public record Resposta<T>(T dados, Instant atualizadoEm) {
        static <T> Resposta<T> agora(T dados) {
            return new Resposta<>(dados, Instant.now());
        }
    }

    public record TemporadaComCampeonatos(TemporadaService.Temporada temporada,
                                          List<CampeonatoService.Campeonato> campeonatos, boolean destaque) {
    }

    public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total) {
    }

    public record TimePublico(TimeService.Time time, List<ElencoService.Elenco> elencos) {
    }

    private static final int LIMITE_SEM_PAGINAR = 200;
    private static final int TAMANHO_PADRAO_DA_PAGINA = 50;

    private final TemporadaService temporadas;
    private final CampeonatoService campeonatos;
    private final ClassificacaoService classificacao;
    private final PartidaService partidas;
    private final TimeService times;
    private final ElencoService elencos;
    private final JogadorService jogadores;

    public PublicoController(TemporadaService temporadas, CampeonatoService campeonatos, ClassificacaoService classificacao,
                             PartidaService partidas, TimeService times, ElencoService elencos, JogadorService jogadores) {
        this.temporadas = temporadas;
        this.campeonatos = campeonatos;
        this.classificacao = classificacao;
        this.partidas = partidas;
        this.times = times;
        this.elencos = elencos;
        this.jogadores = jogadores;
    }

    /** Temporadas da mais recente para a mais antiga; a ativa mais recente vem com {@code destaque}. */
    @GetMapping("/seasons")
    public Resposta<List<TemporadaComCampeonatos>> temporadas() {
        TemporadaService.Temporada ativa = temporadas.ativaMaisRecente();
        List<TemporadaComCampeonatos> lista = temporadas.listar().stream()
                .map(t -> new TemporadaComCampeonatos(t, campeonatos.listarDaTemporada(t.id()),
                        ativa != null && ativa.id().equals(t.id())))
                .toList();
        return Resposta.agora(lista);
    }

    @GetMapping("/seasons/{id}")
    public Resposta<TemporadaComCampeonatos> temporada(@PathVariable UUID id) {
        TemporadaService.Temporada ativa = temporadas.ativaMaisRecente();
        TemporadaService.Temporada t = temporadas.buscar(id);
        return Resposta.agora(new TemporadaComCampeonatos(t, campeonatos.listarDaTemporada(id),
                ativa != null && ativa.id().equals(id)));
    }

    @GetMapping("/championships/{id}")
    public Resposta<CampeonatoService.Campeonato> campeonato(@PathVariable UUID id) {
        return Resposta.agora(campeonatos.buscar(id));
    }

    @GetMapping("/championships/{id}/phases")
    public Resposta<List<CampeonatoService.FaseDto>> fases(@PathVariable UUID id) {
        return Resposta.agora(campeonatos.fasesDo(id));
    }

    /** Calculada a cada consulta a partir das Partidas FINALIZADAS: sem cache e sem tabela guardada. */
    @GetMapping("/phases/{id}/standings")
    public Resposta<ClassificacaoService.Tabela> classificacao(@PathVariable UUID id) {
        return Resposta.agora(classificacao.tabela(id));
    }

    @GetMapping("/championships/{id}/matches")
    public Resposta<List<PartidaService.Partida>> partidas(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID fase,
            @RequestParam(required = false) UUID time,
            @RequestParam(required = false) LocalDate dia) {
        return Resposta.agora(partidas.listarDoCampeonato(id, fase, time, dia));
    }

    @GetMapping("/teams")
    public Resposta<List<TimeService.Time>> times() {
        return Resposta.agora(times.listar());
    }

    @GetMapping("/teams/{id}")
    public Resposta<TimePublico> time(@PathVariable UUID id) {
        return Resposta.agora(new TimePublico(times.buscar(id), elencos.doTime(id)));
    }

    /** Busca por Riot ID. Até 200 itens vem tudo; acima disso, paginado por {@code pagina} e {@code tamanho}. */
    @GetMapping("/players")
    public Resposta<Pagina<JogadorService.Jogador>> jogadores(
            @RequestParam(required = false, defaultValue = "") String busca,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        List<JogadorService.Jogador> todos = jogadores.buscar(busca);
        if (pagina == null && tamanho == null && todos.size() <= LIMITE_SEM_PAGINAR) {
            return Resposta.agora(new Pagina<>(todos, 0, todos.size(), todos.size()));
        }
        int tam = tamanho == null || tamanho < 1 ? TAMANHO_PADRAO_DA_PAGINA : Math.min(tamanho, LIMITE_SEM_PAGINAR);
        int pag = pagina == null || pagina < 0 ? 0 : pagina;
        int de = Math.min(pag * tam, todos.size());
        int ate = Math.min(de + tam, todos.size());
        return Resposta.agora(new Pagina<>(todos.subList(de, ate), pag, tam, todos.size()));
    }

    @GetMapping("/players/{id}")
    public Resposta<JogadorService.Jogador> jogador(@PathVariable UUID id) {
        return Resposta.agora(jogadores.obter(id));
    }
}
