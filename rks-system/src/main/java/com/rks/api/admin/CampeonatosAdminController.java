package com.rks.api.admin;

import com.rks.application.campeonatos.CampeonatoService;
import com.rks.application.campeonatos.TemporadaService;
import com.rks.application.times.ElencoService;
import com.rks.domain.rules.Pontuacao;
import com.rks.domain.standings.CriterioDesempate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Temporadas, Campeonatos e a participação dos Times (histórias 2.15, 2.16, 2.19 e 2.25). */
@RestController
@RequestMapping("/api/admin")
public class CampeonatosAdminController {

    public record TemporadaRequest(@NotBlank String nome, LocalDate dataInicio, LocalDate dataFim) {
    }

    public record PontuacaoRequest(@NotNull Integer vitoria, @NotNull Integer derrotaDecisiva, @NotNull Integer derrota) {
    }

    public record NovoCampeonatoRequest(@NotNull UUID temporadaId, @NotBlank String nome, String descricao,
                                        Integer melhorDe, @Valid PontuacaoRequest pontuacao,
                                        List<CriterioDesempate> criterios) {
    }

    public record CampeonatoRequest(@NotBlank String nome, String descricao, String estado) {
    }

    public record ConfiguracaoRequest(@NotNull Integer melhorDe, @NotNull @Valid PontuacaoRequest pontuacao,
                                      @NotNull List<CriterioDesempate> criterios) {
    }

    public record VinculoRequest(@NotNull UUID timeId) {
    }

    private final TemporadaService temporadas;
    private final CampeonatoService campeonatos;
    private final ElencoService elencos;

    public CampeonatosAdminController(TemporadaService temporadas, CampeonatoService campeonatos, ElencoService elencos) {
        this.temporadas = temporadas;
        this.campeonatos = campeonatos;
        this.elencos = elencos;
    }

    @GetMapping("/seasons")
    public List<TemporadaService.Temporada> listarTemporadas() {
        return temporadas.listar();
    }

    @PostMapping("/seasons")
    @ResponseStatus(HttpStatus.CREATED)
    public TemporadaService.Temporada criarTemporada(@Valid @RequestBody TemporadaRequest pedido) {
        return temporadas.criar(pedido.nome(), pedido.dataInicio(), pedido.dataFim());
    }

    @PutMapping("/seasons/{id}")
    public TemporadaService.Temporada atualizarTemporada(@PathVariable UUID id, @Valid @RequestBody TemporadaRequest pedido) {
        return temporadas.atualizar(id, pedido.nome(), pedido.dataInicio(), pedido.dataFim());
    }

    @PostMapping("/seasons/{id}/close")
    public TemporadaService.Temporada encerrarTemporada(@PathVariable UUID id) {
        return temporadas.definirEncerrada(id, true);
    }

    @PostMapping("/seasons/{id}/reopen")
    public TemporadaService.Temporada reabrirTemporada(@PathVariable UUID id) {
        return temporadas.definirEncerrada(id, false);
    }

    @DeleteMapping("/seasons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirTemporada(@PathVariable UUID id) {
        temporadas.excluir(id);
    }

    @GetMapping("/championships")
    public List<CampeonatoService.Campeonato> listarCampeonatos(@RequestParam UUID temporadaId) {
        return campeonatos.listarDaTemporada(temporadaId);
    }

    @PostMapping("/championships")
    @ResponseStatus(HttpStatus.CREATED)
    public CampeonatoService.Campeonato criarCampeonato(Authentication quem, @Valid @RequestBody NovoCampeonatoRequest pedido) {
        return campeonatos.criar(quem.getName(), pedido.temporadaId(), pedido.nome(), pedido.descricao(),
                pedido.melhorDe(), pontuacao(pedido.pontuacao()), pedido.criterios());
    }

    @GetMapping("/championships/{id}")
    public CampeonatoService.Campeonato buscarCampeonato(@PathVariable UUID id) {
        return campeonatos.buscar(id);
    }

    @PutMapping("/championships/{id}")
    public CampeonatoService.Campeonato atualizarCampeonato(Authentication quem, @PathVariable UUID id,
                                                           @Valid @RequestBody CampeonatoRequest pedido) {
        return campeonatos.atualizar(quem.getName(), id, pedido.nome(), pedido.descricao(), pedido.estado());
    }

    /** Congela com a primeira Partida FINALIZADA: 409 CONFIG_CONGELADA, com o número de Partidas finalizadas. */
    @PutMapping("/championships/{id}/config")
    public CampeonatoService.Campeonato configurar(Authentication quem, @PathVariable UUID id,
                                                  @Valid @RequestBody ConfiguracaoRequest pedido) {
        return campeonatos.atualizarConfiguracao(quem.getName(), id, pedido.melhorDe(), pontuacao(pedido.pontuacao()),
                pedido.criterios());
    }

    @GetMapping("/championships/{id}/teams")
    public List<ElencoService.Participacao> times(@PathVariable UUID id) {
        return elencos.participacoesDoCampeonato(id);
    }

    @PostMapping("/championships/{id}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public ElencoService.Participacao vincular(Authentication quem, @PathVariable UUID id,
                                              @Valid @RequestBody VinculoRequest pedido) {
        return elencos.vincular(quem.getName(), id, pedido.timeId());
    }

    @DeleteMapping("/championships/{id}/teams/{timeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincular(Authentication quem, @PathVariable UUID id, @PathVariable UUID timeId) {
        elencos.desvincular(quem.getName(), id, timeId);
    }

    private static Pontuacao pontuacao(PontuacaoRequest pedido) {
        if (pedido == null) {
            return null;
        }
        try {
            return new Pontuacao(pedido.vitoria(), pedido.derrotaDecisiva(), pedido.derrota());
        } catch (IllegalArgumentException e) {
            throw com.rks.application.erro.ErroDeRegra.regra("PONTUACAO_INVALIDA", e.getMessage());
        }
    }
}
