package com.rks.application.imagens;

import com.rks.application.auditoria.AuditoriaService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.armazenamento.ProcessadorDeImagem;
import com.rks.infra.persistencia.JogadorRepositorio;
import com.rks.infra.persistencia.TimeRepositorio;
import com.rks.infra.persistencia.entidade.JogadorEntidade;
import com.rks.infra.persistencia.entidade.TimeEntidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

/**
 * Logo de Time e foto de Jogador (FR-24, AD-16). O objeto é enviado ANTES e a chave é gravada na mesma
 * transação do restante: falha deixa objeto solto no armazenamento, nunca referência órfã. A imagem
 * antiga só é apagada depois do commit. Nada de {@code uploads/} local nem de bytes no banco.
 */
@Service
public class ImagemService {

    private static final Logger log = LoggerFactory.getLogger(ImagemService.class);

    private final ImageStorage armazenamento;
    private final TimeRepositorio times;
    private final JogadorRepositorio jogadores;
    private final AuditoriaService auditoria;

    public ImagemService(ImageStorage armazenamento, TimeRepositorio times, JogadorRepositorio jogadores,
                         AuditoriaService auditoria) {
        this.armazenamento = armazenamento;
        this.times = times;
        this.jogadores = jogadores;
        this.auditoria = auditoria;
    }

    @Transactional
    public void enviarLogo(String autor, UUID timeId, byte[] dados) {
        TimeEntidade time = times.findById(timeId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("TIME_NAO_ENCONTRADO", "Time não encontrado."));
        String antiga = time.getLogoUrl();
        String nova = enviar("times/" + timeId, dados);
        time.setLogoUrl(nova);
        times.save(time);
        apagarDepoisDoCommit(antiga);
        auditoria.registrar(autor, "time", timeId, "LOGO", Map.of("logo", String.valueOf(antiga)), Map.of("logo", nova));
    }

    @Transactional
    public void removerLogo(String autor, UUID timeId) {
        TimeEntidade time = times.findById(timeId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("TIME_NAO_ENCONTRADO", "Time não encontrado."));
        String antiga = time.getLogoUrl();
        time.setLogoUrl(null);
        times.save(time);
        apagarDepoisDoCommit(antiga);
        auditoria.registrar(autor, "time", timeId, "LOGO_REMOVIDO", Map.of("logo", String.valueOf(antiga)), null);
    }

    @Transactional
    public void enviarFoto(String autor, UUID jogadorId, byte[] dados) {
        JogadorEntidade jogador = jogadores.findById(jogadorId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("JOGADOR_NAO_ENCONTRADO", "Jogador não encontrado."));
        String antiga = jogador.getFotoUrl();
        String nova = enviar("jogadores/" + jogadorId, dados);
        jogador.setFotoUrl(nova);
        jogadores.save(jogador);
        apagarDepoisDoCommit(antiga);
        auditoria.registrar(autor, "jogador", jogadorId, "FOTO", Map.of("foto", String.valueOf(antiga)), Map.of("foto", nova));
    }

    @Transactional
    public void removerFoto(String autor, UUID jogadorId) {
        JogadorEntidade jogador = jogadores.findById(jogadorId)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("JOGADOR_NAO_ENCONTRADO", "Jogador não encontrado."));
        String antiga = jogador.getFotoUrl();
        jogador.setFotoUrl(null);
        jogadores.save(jogador);
        apagarDepoisDoCommit(antiga);
        auditoria.registrar(autor, "jogador", jogadorId, "FOTO_REMOVIDA", Map.of("foto", String.valueOf(antiga)), null);
    }

    /** Valida pelo conteúdo, regrava e envia. A chave é única por envio, então nunca sobrescreve a anterior. */
    private String enviar(String prefixo, byte[] dados) {
        ProcessadorDeImagem.Processada imagem = ProcessadorDeImagem.processar(dados);
        String chave = prefixo + "/" + UUID.randomUUID() + "." + imagem.extensao();
        armazenamento.put(chave, imagem.conteudo(), imagem.contentType());
        return chave;
    }

    private void apagarDepoisDoCommit(String chave) {
        if (chave == null || chave.isBlank()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    armazenamento.delete(chave);
                } catch (RuntimeException e) {
                    log.warn("Imagem antiga ficou solta no armazenamento: {}", chave);
                }
            }
        });
    }
}
