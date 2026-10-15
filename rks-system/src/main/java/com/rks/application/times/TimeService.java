package com.rks.application.times;

import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.ParticipacaoRepositorio;
import com.rks.infra.persistencia.TimeRepositorio;
import com.rks.infra.persistencia.entidade.TimeEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Gestão de Times (FR-4): tag única sem diferenciar maiúsculas; excluir é bloqueado com Partidas finalizadas. */
@Service
public class TimeService {

    public record Time(UUID id, String nome, String tag, String logoUrl, Instant criadoEm) {
    }

    private final TimeRepositorio times;
    private final ParticipacaoRepositorio participacoes;

    public TimeService(TimeRepositorio times, ParticipacaoRepositorio participacoes) {
        this.times = times;
        this.participacoes = participacoes;
    }

    @Transactional(readOnly = true)
    public List<Time> listar() {
        return times.findAllByOrderByNomeAsc().stream().map(TimeService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public Time buscar(UUID id) {
        return paraDto(obter(id));
    }

    @Transactional
    public Time criar(String nome, String tag) {
        TimeEntidade t = new TimeEntidade();
        preencher(t, nome, tag);
        return paraDto(times.save(t));
    }

    @Transactional
    public Time atualizar(UUID id, String nome, String tag) {
        TimeEntidade t = obter(id);
        preencher(t, nome, tag);
        return paraDto(times.save(t));
    }

    @Transactional
    public void excluir(UUID id) {
        TimeEntidade t = obter(id);
        long finalizadas = times.contarPartidasFinalizadas(id);
        if (finalizadas > 0) {
            throw ErroDeRegra.conflito("TIME_COM_PARTIDAS_FINALIZADAS",
                    "O Time tem " + finalizadas + " Partidas finalizadas e não pode ser excluído.",
                    Map.of("partidasFinalizadas", finalizadas));
        }
        if (!participacoes.findByTimeId(id).isEmpty()) {
            throw ErroDeRegra.conflito("TIME_EM_CAMPEONATO", "Remova o Time dos Campeonatos antes de excluí-lo.");
        }
        times.delete(t);
    }

    private TimeEntidade obter(UUID id) {
        return times.findById(id).orElseThrow(() -> ErroDeRegra.naoEncontrado("TIME_NAO_ENCONTRADO", "Time não encontrado."));
    }

    private void preencher(TimeEntidade t, String nome, String tag) {
        String nomeLimpo = nome == null ? "" : nome.strip();
        String tagLimpa = tag == null ? "" : tag.strip();
        if (nomeLimpo.isEmpty() || nomeLimpo.length() > 120) {
            throw ErroDeRegra.regra("NOME_INVALIDO", "O nome do Time é obrigatório e vai até 120 caracteres.");
        }
        if (tagLimpa.isEmpty() || tagLimpa.length() > 10) {
            throw ErroDeRegra.regra("TAG_INVALIDA", "A tag do Time é obrigatória e vai até 10 caracteres.");
        }
        String chave = tagLimpa.toLowerCase(Locale.ROOT);
        times.findByTagKey(chave).filter(outro -> !outro.getId().equals(t.getId())).ifPresent(outro -> {
            throw ErroDeRegra.conflito("TAG_JA_CADASTRADA", "Já existe um Time com a tag " + tagLimpa + ".",
                    Map.of("timeId", outro.getId()));
        });
        t.setNome(nomeLimpo);
        t.setTag(tagLimpa);
        t.setTagKey(chave);
    }

    static Time paraDto(TimeEntidade t) {
        return new Time(t.getId(), t.getNome(), t.getTag(), t.getLogoUrl(), t.getCriadoEm());
    }
}
