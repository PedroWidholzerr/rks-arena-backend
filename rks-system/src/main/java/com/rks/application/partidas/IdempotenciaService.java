package com.rks.application.partidas;

import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.ChaveIdempotenciaRepositorio;
import com.rks.infra.persistencia.entidade.ChaveIdempotenciaEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Idempotência de escrita (AD-23): repetir a mesma chave (cold start, duplo clique) devolve a MESMA
 * resposta sem gravar de novo; a mesma chave com corpo ou rota diferente é 422 CHAVE_REUTILIZADA.
 * A resposta é guardada por 24 h na mesma transação da ação, então ou os dois ficam, ou nenhum.
 * Uma trava de transação por chave serializa pedidos simultâneos com a mesma chave.
 */
@Service
public class IdempotenciaService {

    public static final Duration VALIDADE = Duration.ofHours(24);

    private final ChaveIdempotenciaRepositorio chaves;
    private final ObjectMapper json;

    public IdempotenciaService(ChaveIdempotenciaRepositorio chaves, ObjectMapper json) {
        this.chaves = chaves;
        this.json = json;
    }

    /** O escopo identifica rota e corpo: a mesma chave só se repete para o mesmo pedido. */
    public String escopo(String rota, Object corpo) {
        // A coluna escopo tem 80 caracteres: guarda só o hash de rota e corpo (64 hexadecimais).
        return sha256(rota + "#" + json.writeValueAsString(corpo));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public <T> T executar(UUID chave, String escopo, Class<T> tipo, Supplier<T> acao) {
        chaves.travar(chave.toString());
        List<ChaveIdempotenciaEntidade> existentes = chaves.findByChave(chave);
        for (ChaveIdempotenciaEntidade existente : existentes) {
            if (existente.getEm().isBefore(Instant.now().minus(VALIDADE))) {
                chaves.delete(existente);
                continue;
            }
            if (!existente.getEscopo().equals(escopo)) {
                throw ErroDeRegra.regra("CHAVE_REUTILIZADA",
                        "Esta Idempotency-Key já foi usada em outro pedido. Gere uma chave nova.");
            }
            return json.readValue(existente.getCorpo(), tipo);
        }
        T resultado = acao.get();
        ChaveIdempotenciaEntidade nova = new ChaveIdempotenciaEntidade();
        nova.setChave(chave);
        nova.setEscopo(escopo);
        nova.setStatus((short) 200);
        nova.setCorpo(json.writeValueAsString(resultado));
        chaves.save(nova);
        return resultado;
    }

    /** Limpeza das chaves vencidas, chamada na varredura semanal. */
    @Transactional
    public int limparVencidas() {
        return chaves.apagarAnterioresA(Instant.now().minus(VALIDADE));
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
