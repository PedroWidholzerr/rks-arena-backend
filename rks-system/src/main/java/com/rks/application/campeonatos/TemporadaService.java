package com.rks.application.campeonatos;

import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.CampeonatoRepositorio;
import com.rks.infra.persistencia.TemporadaRepositorio;
import com.rks.infra.persistencia.entidade.TemporadaEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Gestão de Temporadas (FR-1). Excluir é bloqueado quando há Partidas finalizadas; o caminho é encerrar. */
@Service
public class TemporadaService {

    public record Temporada(UUID id, String nome, LocalDate dataInicio, LocalDate dataFim, boolean encerrada,
                            Instant criadoEm) {
    }

    private final TemporadaRepositorio temporadas;
    private final CampeonatoRepositorio campeonatos;

    public TemporadaService(TemporadaRepositorio temporadas, CampeonatoRepositorio campeonatos) {
        this.temporadas = temporadas;
        this.campeonatos = campeonatos;
    }

    /** Da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<Temporada> listar() {
        return temporadas.listarDaMaisRecente().stream().map(TemporadaService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public Temporada buscar(UUID id) {
        return paraDto(obter(id));
    }

    /** A Temporada ativa mais recente, destacada na página inicial. */
    @Transactional(readOnly = true)
    public Temporada ativaMaisRecente() {
        return temporadas.listarDaMaisRecente().stream()
                .filter(t -> !t.isEncerrada())
                .findFirst()
                .map(TemporadaService::paraDto)
                .orElse(null);
    }

    @Transactional
    public Temporada criar(String nome, LocalDate dataInicio, LocalDate dataFim) {
        TemporadaEntidade t = new TemporadaEntidade();
        preencher(t, nome, dataInicio, dataFim);
        return paraDto(temporadas.save(t));
    }

    @Transactional
    public Temporada atualizar(UUID id, String nome, LocalDate dataInicio, LocalDate dataFim) {
        TemporadaEntidade t = obter(id);
        preencher(t, nome, dataInicio, dataFim);
        return paraDto(temporadas.save(t));
    }

    @Transactional
    public Temporada definirEncerrada(UUID id, boolean encerrada) {
        TemporadaEntidade t = obter(id);
        t.setEncerrada(encerrada);
        return paraDto(temporadas.save(t));
    }

    @Transactional
    public void excluir(UUID id) {
        TemporadaEntidade t = obter(id);
        long finalizadas = temporadas.contarPartidasFinalizadas(id);
        if (finalizadas > 0) {
            throw ErroDeRegra.conflito("TEMPORADA_COM_PARTIDAS_FINALIZADAS",
                    "A Temporada tem " + finalizadas + " Partidas finalizadas e não pode ser excluída; encerre-a.",
                    java.util.Map.of("partidasFinalizadas", finalizadas));
        }
        if (!campeonatos.findByTemporadaIdOrderByCriadoEmDesc(id).isEmpty()) {
            throw ErroDeRegra.conflito("TEMPORADA_COM_CAMPEONATOS",
                    "A Temporada ainda tem Campeonatos; encerre-a em vez de excluir.");
        }
        temporadas.delete(t);
    }

    private TemporadaEntidade obter(UUID id) {
        return temporadas.findById(id)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("TEMPORADA_NAO_ENCONTRADA", "Temporada não encontrada."));
    }

    private static void preencher(TemporadaEntidade t, String nome, LocalDate inicio, LocalDate fim) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty() || limpo.length() > 120) {
            throw ErroDeRegra.regra("NOME_INVALIDO", "O nome da Temporada é obrigatório e vai até 120 caracteres.");
        }
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw ErroDeRegra.regra("DATAS_INVALIDAS", "A data final não pode ser anterior à inicial.");
        }
        t.setNome(limpo);
        t.setDataInicio(inicio);
        t.setDataFim(fim);
    }

    static Temporada paraDto(TemporadaEntidade t) {
        return new Temporada(t.getId(), t.getNome(), t.getDataInicio(), t.getDataFim(), t.isEncerrada(), t.getCriadoEm());
    }
}
