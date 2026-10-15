package com.rks.api.admin;

import com.rks.application.classificacao.ClassificacaoService;
import com.rks.application.partidas.IdempotenciaService;
import com.rks.application.partidas.PartidaService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Partidas, resultados e desempate (histórias 2.20 a 2.24 e 2.26). */
@RestController
@RequestMapping("/api/admin")
public class PartidasAdminController {

    public record NovaPartidaRequest(@NotNull UUID faseId, @NotNull UUID timeAId, @NotNull UUID timeBId, Integer rodada,
                                     Instant agendadaPara, String streamUrl) {
    }

    public record PartidaRequest(@NotNull Long version, @NotNull UUID timeAId, @NotNull UUID timeBId,
                                 Instant agendadaPara, String streamUrl) {
    }

    public record VersaoRequest(@NotNull Long version) {
    }

    public record ResultadoRequest(@NotNull Long version, @NotNull Integer jogosA, @NotNull Integer jogosB, UUID mvpJogadorId) {
    }

    public record WalkoverRequest(@NotNull Long version, @NotBlank String vencedor) {
    }

    public record DecisaoRequest(@NotNull List<UUID> ordem, @NotBlank String motivo) {
    }

    private final PartidaService partidas;
    private final IdempotenciaService idempotencia;
    private final ClassificacaoService classificacao;

    public PartidasAdminController(PartidaService partidas, IdempotenciaService idempotencia,
                                   ClassificacaoService classificacao) {
        this.partidas = partidas;
        this.idempotencia = idempotencia;
        this.classificacao = classificacao;
    }

    @GetMapping("/matches")
    public List<PartidaService.Partida> listar(@RequestParam UUID faseId) {
        return partidas.listarDaFase(faseId);
    }

    @GetMapping("/matches/{id}")
    public PartidaService.Partida buscar(@PathVariable UUID id) {
        return partidas.buscar(id);
    }

    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public PartidaService.Partida agendar(Authentication quem, @Valid @RequestBody NovaPartidaRequest pedido) {
        return partidas.agendar(quem.getName(), pedido.faseId(), pedido.timeAId(), pedido.timeBId(), pedido.rodada(),
                pedido.agendadaPara(), pedido.streamUrl());
    }

    @PutMapping("/matches/{id}")
    public PartidaService.Partida atualizar(Authentication quem, @PathVariable UUID id, @Valid @RequestBody PartidaRequest pedido) {
        return partidas.atualizar(quem.getName(), id, pedido.version(), pedido.timeAId(), pedido.timeBId(),
                pedido.agendadaPara(), pedido.streamUrl());
    }

    @DeleteMapping("/matches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(Authentication quem, @PathVariable UUID id) {
        partidas.excluir(quem.getName(), id);
    }

    @PostMapping("/matches/{id}/start")
    public PartidaService.Partida iniciar(Authentication quem, @PathVariable UUID id, @Valid @RequestBody VersaoRequest pedido) {
        return partidas.iniciar(quem.getName(), id, pedido.version());
    }

    /**
     * Lança ou edita o resultado. Exige {@code Idempotency-Key}: repetir a chave devolve a mesma resposta
     * sem gravar duas vezes; a mesma chave com corpo diferente é 422 CHAVE_REUTILIZADA (AD-23).
     */
    @PutMapping("/matches/{id}/result")
    public PartidaService.Partida resultado(Authentication quem, @PathVariable UUID id,
                                            @RequestHeader("Idempotency-Key") UUID chave,
                                            @Valid @RequestBody ResultadoRequest pedido) {
        String escopo = idempotencia.escopo("PUT /api/admin/matches/" + id + "/result", pedido);
        return idempotencia.executar(chave, escopo, PartidaService.Partida.class,
                () -> partidas.lancarResultado(quem.getName(), id, pedido.version(), pedido.jogosA(), pedido.jogosB(),
                        pedido.mvpJogadorId()));
    }

    @PutMapping("/matches/{id}/walkover")
    public PartidaService.Partida walkover(Authentication quem, @PathVariable UUID id, @Valid @RequestBody WalkoverRequest pedido) {
        boolean vencedorEhA = switch (pedido.vencedor().toUpperCase()) {
            case "A" -> true;
            case "B" -> false;
            default -> throw com.rks.application.erro.ErroDeRegra.regra("VENCEDOR_INVALIDO", "O vencedor deve ser A ou B.");
        };
        return partidas.registrarWalkover(quem.getName(), id, pedido.version(), vencedorEhA);
    }

    /** Anular resultado: volta a AGENDADA, sem placar. Não confundir com cancelar a Partida (O3). */
    @PutMapping("/matches/{id}/annul")
    public PartidaService.Partida anular(Authentication quem, @PathVariable UUID id, @Valid @RequestBody VersaoRequest pedido) {
        return partidas.anularResultado(quem.getName(), id, pedido.version());
    }

    @PostMapping("/phases/{id}/tie-decisions")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassificacaoService.Tabela decidirEmpate(Authentication quem, @PathVariable UUID id,
                                                     @Valid @RequestBody DecisaoRequest pedido) {
        return classificacao.decidirEmpate(quem.getName(), id, pedido.ordem(), pedido.motivo());
    }

    @PostMapping("/phases/{id}/recalculate")
    public ClassificacaoService.Recalculo recalcular(@PathVariable UUID id) {
        return classificacao.recalcular(id);
    }
}
