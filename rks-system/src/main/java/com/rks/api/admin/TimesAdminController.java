package com.rks.api.admin;

import com.rks.application.times.ElencoService;
import com.rks.application.times.JogadorService;
import com.rks.application.times.TimeService;
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

import java.util.List;
import java.util.UUID;

/** Times, Jogadores e Elenco da Temporada (histórias 2.17, 2.18 e 2.19). */
@RestController
@RequestMapping("/api/admin")
public class TimesAdminController {

    public record TimeRequest(@NotBlank String nome, @NotBlank String tag) {
    }

    public record JogadorRequest(@NotBlank String nick, @NotBlank String tag) {
    }

    public record MembroRequest(@NotNull UUID jogadorId, @NotBlank String funcao, boolean capitao) {
    }

    public record ElencoRequest(@NotNull @Valid List<MembroRequest> membros) {
    }

    private final TimeService times;
    private final JogadorService jogadores;
    private final ElencoService elencos;

    public TimesAdminController(TimeService times, JogadorService jogadores, ElencoService elencos) {
        this.times = times;
        this.jogadores = jogadores;
        this.elencos = elencos;
    }

    @GetMapping("/teams")
    public List<TimeService.Time> listarTimes() {
        return times.listar();
    }

    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeService.Time criarTime(@Valid @RequestBody TimeRequest pedido) {
        return times.criar(pedido.nome(), pedido.tag());
    }

    @GetMapping("/teams/{id}")
    public TimeService.Time buscarTime(@PathVariable UUID id) {
        return times.buscar(id);
    }

    @PutMapping("/teams/{id}")
    public TimeService.Time atualizarTime(@PathVariable UUID id, @Valid @RequestBody TimeRequest pedido) {
        return times.atualizar(id, pedido.nome(), pedido.tag());
    }

    @DeleteMapping("/teams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirTime(@PathVariable UUID id) {
        times.excluir(id);
    }

    @GetMapping("/teams/{id}/rosters/{temporadaId}")
    public ElencoService.Elenco buscarElenco(@PathVariable UUID id, @PathVariable UUID temporadaId) {
        return elencos.buscar(id, temporadaId);
    }

    /** Comando agregado "salvar elenco": valida tamanho e Capitão antes de confirmar. */
    @PutMapping("/teams/{id}/rosters/{temporadaId}")
    public ElencoService.Elenco salvarElenco(Authentication quem, @PathVariable UUID id, @PathVariable UUID temporadaId,
                                             @Valid @RequestBody ElencoRequest pedido) {
        List<ElencoService.MembroPedido> membros = pedido.membros().stream()
                .map(m -> new ElencoService.MembroPedido(m.jogadorId(), m.funcao(), m.capitao())).toList();
        return elencos.salvar(quem.getName(), id, temporadaId, membros);
    }

    @GetMapping("/players")
    public List<JogadorService.Jogador> listarJogadores(@RequestParam(required = false, defaultValue = "") String busca) {
        return jogadores.buscar(busca);
    }

    @PostMapping("/players")
    @ResponseStatus(HttpStatus.CREATED)
    public JogadorService.Jogador criarJogador(@Valid @RequestBody JogadorRequest pedido) {
        return jogadores.criar(pedido.nick(), pedido.tag());
    }

    @PutMapping("/players/{id}")
    public JogadorService.Jogador atualizarJogador(@PathVariable UUID id, @Valid @RequestBody JogadorRequest pedido) {
        return jogadores.atualizar(id, pedido.nick(), pedido.tag());
    }
}
