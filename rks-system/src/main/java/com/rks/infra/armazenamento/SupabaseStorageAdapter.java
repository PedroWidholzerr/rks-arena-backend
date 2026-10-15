package com.rks.infra.armazenamento;

import com.rks.application.erro.ErroDeRegra;
import com.rks.application.imagens.ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Adaptador do Supabase Storage (AD-16). Usa o HttpClient do JDK, sem dependência nova. A chave de
 * serviço só existe no backend (SUPABASE_SERVICE_KEY) e nunca vai a log, DTO ou front. O bucket é
 * público (rks-img): o navegador lê a imagem direto da URL pública.
 */
@Component
public class SupabaseStorageAdapter implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageAdapter.class);

    private final String urlBase;
    private final String chaveDeServico;
    private final String bucket;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public SupabaseStorageAdapter(
            @Value("${app.supabase.url:}") String urlBase,
            @Value("${app.supabase.service-key:}") String chaveDeServico,
            @Value("${app.supabase.bucket:rks-img}") String bucket) {
        this.urlBase = urlBase == null ? "" : urlBase.strip().replaceAll("/+$", "");
        this.chaveDeServico = chaveDeServico == null ? "" : chaveDeServico.strip();
        this.bucket = bucket;
    }

    @Override
    public void put(String chave, byte[] conteudo, String contentType) {
        exigirConfigurado();
        HttpRequest pedido = comAutorizacao(HttpRequest.newBuilder(URI.create(urlDoObjeto(chave))))
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(conteudo))
                .build();
        HttpResponse<Void> resposta = enviar(pedido);
        if (resposta.statusCode() / 100 != 2) {
            log.error("Falha ao enviar imagem ao armazenamento: HTTP {}", resposta.statusCode());
            throw ErroDeRegra.indisponivel("ARMAZENAMENTO_INDISPONIVEL",
                    "Não foi possível enviar a imagem agora. Tente de novo em instantes.");
        }
    }

    @Override
    public void delete(String chave) {
        if (chave == null || chave.isBlank()) {
            return;
        }
        exigirConfigurado();
        HttpRequest pedido = comAutorizacao(HttpRequest.newBuilder(URI.create(urlDoObjeto(chave))))
                .timeout(Duration.ofSeconds(15))
                .DELETE()
                .build();
        HttpResponse<Void> resposta = enviar(pedido);
        if (resposta.statusCode() / 100 != 2 && resposta.statusCode() != 404 && resposta.statusCode() != 400) {
            log.warn("Não foi possível apagar a imagem antiga: HTTP {}", resposta.statusCode());
        }
    }

    @Override
    public String publicUrl(String chave) {
        if (chave == null || chave.isBlank()) {
            return null;
        }
        return urlBase + "/storage/v1/object/public/" + bucket + "/" + codificar(chave);
    }

    private String urlDoObjeto(String chave) {
        return urlBase + "/storage/v1/object/" + bucket + "/" + codificar(chave);
    }

    private HttpRequest.Builder comAutorizacao(HttpRequest.Builder construtor) {
        return construtor.header("apikey", chaveDeServico).header("Authorization", "Bearer " + chaveDeServico);
    }

    private HttpResponse<Void> enviar(HttpRequest pedido) {
        try {
            return http.send(pedido, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            log.error("Armazenamento de imagens inacessível: {}", e.getClass().getSimpleName());
            throw ErroDeRegra.indisponivel("ARMAZENAMENTO_INDISPONIVEL",
                    "Não foi possível enviar a imagem agora. Tente de novo em instantes.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ErroDeRegra.indisponivel("ARMAZENAMENTO_INDISPONIVEL", "O envio da imagem foi interrompido.");
        }
    }

    private void exigirConfigurado() {
        if (urlBase.isEmpty() || chaveDeServico.isEmpty()) {
            throw ErroDeRegra.indisponivel("ARMAZENAMENTO_INDISPONIVEL",
                    "O armazenamento de imagens não está configurado (SUPABASE_URL e SUPABASE_SERVICE_KEY).");
        }
    }

    /** Codifica cada trecho do caminho, preservando as barras. */
    private static String codificar(String chave) {
        return Arrays.stream(chave.split("/"))
                .map(parte -> URLEncoder.encode(parte, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }
}
