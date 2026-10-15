package com.rks.infra.armazenamento;

import com.rks.application.erro.ErroDeRegra;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Simula o Storage do Supabase com o servidor HTTP do próprio JDK: nada de rede nem de conta. */
class SupabaseStorageAdapterTest {

    private HttpServer servidor;
    private String url;
    private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();
    private final Map<String, String> cabecalhos = new ConcurrentHashMap<>();
    private final AtomicInteger statusForcado = new AtomicInteger(0);

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/storage/v1/object/rks-img/", troca -> {
            String chave = troca.getRequestURI().getPath().substring("/storage/v1/object/rks-img/".length());
            cabecalhos.put("apikey", String.valueOf(troca.getRequestHeaders().getFirst("apikey")));
            cabecalhos.put("authorization", String.valueOf(troca.getRequestHeaders().getFirst("Authorization")));
            cabecalhos.put("content-type", String.valueOf(troca.getRequestHeaders().getFirst("Content-Type")));
            int status = statusForcado.get();
            if (status == 0) {
                if (troca.getRequestMethod().equals("POST")) {
                    objetos.put(chave, troca.getRequestBody().readAllBytes());
                    status = 200;
                } else if (troca.getRequestMethod().equals("DELETE")) {
                    status = objetos.remove(chave) != null ? 200 : 404;
                }
            }
            troca.sendResponseHeaders(status, -1);
            troca.close();
        });
        servidor.start();
        url = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void descer() {
        servidor.stop(0);
    }

    @Test
    void enviaOObjetoComAChaveDeServicoEOTipoCerto() {
        SupabaseStorageAdapter adaptador = new SupabaseStorageAdapter(url, "chave-de-teste", "rks-img");
        byte[] bytes = {1, 2, 3, 4};

        adaptador.put("times/abc/logo.png", bytes, "image/png");

        assertArrayEquals(bytes, objetos.get("times/abc/logo.png"));
        assertEquals("chave-de-teste", cabecalhos.get("apikey"));
        assertEquals("Bearer chave-de-teste", cabecalhos.get("authorization"));
        assertEquals("image/png", cabecalhos.get("content-type"));
    }

    @Test
    void apagaOObjetoEIgnoraOQueNaoExiste() {
        SupabaseStorageAdapter adaptador = new SupabaseStorageAdapter(url, "k", "rks-img");
        adaptador.put("jogadores/x/foto.jpg", new byte[]{9}, "image/jpeg");

        adaptador.delete("jogadores/x/foto.jpg");
        adaptador.delete("jogadores/x/nao-existe.jpg");

        assertTrue(objetos.isEmpty());
    }

    @Test
    void falhaDoArmazenamentoVira503SemVazarDetalhes() {
        SupabaseStorageAdapter adaptador = new SupabaseStorageAdapter(url, "k", "rks-img");
        statusForcado.set(500);

        ErroDeRegra erro = assertThrows(ErroDeRegra.class, () -> adaptador.put("times/a/b.png", new byte[]{1}, "image/png"));

        assertEquals(ErroDeRegra.Tipo.INDISPONIVEL, erro.tipo());
        assertEquals("ARMAZENAMENTO_INDISPONIVEL", erro.codigo());
    }

    @Test
    void servidorForaDoArVira503() {
        SupabaseStorageAdapter adaptador = new SupabaseStorageAdapter("http://127.0.0.1:1", "k", "rks-img");

        assertEquals("ARMAZENAMENTO_INDISPONIVEL",
                assertThrows(ErroDeRegra.class, () -> adaptador.put("times/a/b.png", new byte[]{1}, "image/png")).codigo());
    }

    @Test
    void semConfiguracaoDiz503ComMensagemClara() {
        SupabaseStorageAdapter adaptador = new SupabaseStorageAdapter("", "", "rks-img");

        ErroDeRegra erro = assertThrows(ErroDeRegra.class, () -> adaptador.put("a/b.png", new byte[]{1}, "image/png"));

        assertTrue(erro.getMessage().contains("SUPABASE_URL"));
        assertNull(adaptador.publicUrl(null));
    }

    @Test
    void urlPublicaAponta1ParaOBucketPublico() {
        SupabaseStorageAdapter adaptador = new SupabaseStorageAdapter("https://abc.supabase.co/", "k", "rks-img");

        assertEquals("https://abc.supabase.co/storage/v1/object/public/rks-img/times/1/logo%20um.png",
                adaptador.publicUrl("times/1/logo um.png"));
    }
}
