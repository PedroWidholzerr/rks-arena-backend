package com.rks.integracao;

import com.rks.Application;
import com.rks.application.administradores.AdministradorService;
import com.rks.application.auditoria.AuditoriaService;
import com.rks.application.campeonatos.CampeonatoService;
import com.rks.application.campeonatos.TemporadaService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.application.imagens.ImageStorage;
import com.rks.application.imagens.ImagemService;
import com.rks.application.partidas.IdempotenciaService;
import com.rks.application.partidas.PartidaService;
import com.rks.application.times.ElencoService;
import com.rks.application.times.JogadorService;
import com.rks.application.times.TimeService;
import com.rks.domain.rules.PlacarInvalidoException;
import com.rks.domain.rules.Pontuacao;
import com.rks.domain.standings.RegrasClassificacao;
import com.rks.infra.persistencia.AuditLogRepositorio;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comportamentos que só um Postgres de verdade prova: trava de transação, atomicidade da auditoria, trava
 * FOR UPDATE do congelamento (AD-10) e o ciclo de imagem que sobrevive a reinício (AD-16). Roda no perfil
 * "integracao" (precisa de Docker).
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
@ActiveProfiles("integracao")
class ComportamentoComBancoIntegracaoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    /** Armazenamento externo simulado. É estático de propósito: sobrevive ao reinício da aplicação, como o Supabase. */
    static final Map<String, byte[]> ARMAZENAMENTO = new ConcurrentHashMap<>();
    private static final AtomicInteger CONTADOR = new AtomicInteger();

    @TestConfiguration
    static class ConfiguracaoDeTeste {
        @Bean
        @Primary
        ImageStorage armazenamentoEmMemoria() {
            return new ImageStorage() {
                @Override
                public void put(String chave, byte[] conteudo, String contentType) {
                    ARMAZENAMENTO.put(chave, conteudo.clone());
                }

                @Override
                public void delete(String chave) {
                    ARMAZENAMENTO.remove(chave);
                }

                @Override
                public String publicUrl(String chave) {
                    return chave == null ? null : "http://armazenamento.teste/rks-img/" + chave;
                }
            };
        }
    }

    @Autowired AdministradorService administradores;
    @Autowired AuditoriaService auditoria;
    @Autowired AuditLogRepositorio registrosDeAuditoria;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired TransactionTemplate transacao;
    @Autowired TemporadaService temporadas;
    @Autowired CampeonatoService campeonatos;
    @Autowired TimeService times;
    @Autowired JogadorService jogadores;
    @Autowired ElencoService elencos;
    @Autowired PartidaService partidas;
    @Autowired IdempotenciaService idempotencia;
    @Autowired ImagemService imagens;

    // ---- administradores e auditoria ----

    @Test
    void tetoDeAdministradoresAtivosVaiAteTresSobConcorrencia() throws Exception {
        usuarios.deleteAll();
        usuarios.save(new UsuarioEntidade("primeiro@rks.teste", "hash", false));

        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String email = "concorrente" + i + "@rks.teste";
            resultados.add(pool.submit(() -> {
                largada.await();
                try {
                    administradores.criar(null, email);
                    return true;
                } catch (ErroDeRegra e) {
                    assertEquals("LIMITE_DE_ADMINISTRADORES", e.codigo());
                    return false;
                }
            }));
        }
        largada.countDown();
        int criados = 0;
        for (Future<Boolean> f : resultados) {
            if (f.get()) criados++;
        }
        pool.shutdown();

        assertEquals(2, criados, "com 1 ativo e teto de 3, só 2 criações simultâneas podem passar");
        assertEquals(3, usuarios.countByAtivoTrue());
    }

    @Test
    void auditoriaSomeQuandoATransacaoFalhaEFicaQuandoConclui() {
        long antes = registrosDeAuditoria.count();

        assertThrows(IllegalStateException.class, () -> transacao.executeWithoutResult(status -> {
            auditoria.registrar(null, "teste", UUID.randomUUID(), "FALHA", null, Map.of("x", 1));
            throw new IllegalStateException("falha depois de auditar");
        }));
        assertEquals(antes, registrosDeAuditoria.count(), "se a transação falha, nenhum registro de auditoria permanece");

        transacao.executeWithoutResult(status -> auditoria.registrar(null, "teste", UUID.randomUUID(), "OK", null, Map.of("x", 2)));
        assertEquals(antes + 1, registrosDeAuditoria.count());
    }

    @Test
    void auditoriaForaDeTransacaoEhRecusada() {
        // propagação MANDATORY: sem transação do caso de uso, não grava nada solto
        assertThrows(Exception.class, () -> auditoria.registrar(null, "teste", UUID.randomUUID(), "SOLTA", null, null));
    }

    // ---- congelamento da configuração (AD-10) ----

    @Test
    void editarARegraELancarResultadoUsamAMesmaTravaEUmDosDoisFalha() throws Exception {
        for (int tentativa = 0; tentativa < 15; tentativa++) {
            Cenario c = criarCenario(3);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch largada = new CountDownLatch(1);

            // Mudar para melhor de 5 e lançar 2x0 (válido só na melhor de 3) não podem ter sucesso juntos:
            // se o resultado entra primeiro, a regra congela; se a regra muda primeiro, 2x0 deixa de valer.
            Callable<Throwable> mudarRegra = () -> {
                largada.await();
                try {
                    campeonatos.atualizarConfiguracao(null, c.campeonato, 5, Pontuacao.PADRAO, RegrasClassificacao.CRITERIOS_PADRAO);
                    return null;
                } catch (Throwable e) {
                    return e;
                }
            };
            Callable<Throwable> lancar = () -> {
                largada.await();
                try {
                    partidas.lancarResultado(null, c.partida, 0, 2, 0, null);
                    return null;
                } catch (Throwable e) {
                    return e;
                }
            };
            Future<Throwable> a = pool.submit(mudarRegra);
            Future<Throwable> b = pool.submit(lancar);
            largada.countDown();
            Throwable erroDaRegra = a.get();
            Throwable erroDoResultado = b.get();
            pool.shutdown();

            int falhas = (erroDaRegra != null ? 1 : 0) + (erroDoResultado != null ? 1 : 0);
            assertEquals(1, falhas, "tentativa " + tentativa + ": exatamente um dos dois deve falhar (regra=" + erroDaRegra + ", resultado=" + erroDoResultado + ")");
            if (erroDaRegra != null) {
                assertTrue(erroDaRegra instanceof ErroDeRegra e && e.codigo().equals("CONFIG_CONGELADA"), String.valueOf(erroDaRegra));
            } else {
                assertTrue(erroDoResultado instanceof PlacarInvalidoException, String.valueOf(erroDoResultado));
            }
        }
    }

    @Test
    void anularOsResultadosLiberaAEdicaoDaRegra() {
        Cenario c = criarCenario(3);
        PartidaService.Partida lancada = partidas.lancarResultado(null, c.partida, 0, 2, 1, null);
        ErroDeRegra congelada = assertThrows(ErroDeRegra.class,
                () -> campeonatos.atualizarConfiguracao(null, c.campeonato, 5, Pontuacao.PADRAO, RegrasClassificacao.CRITERIOS_PADRAO));
        assertEquals("CONFIG_CONGELADA", congelada.codigo());
        assertEquals(1L, congelada.extras().get("partidasFinalizadas"));

        partidas.anularResultado(null, c.partida, lancada.version());
        CampeonatoService.Campeonato editado =
                campeonatos.atualizarConfiguracao(null, c.campeonato, 5, Pontuacao.PADRAO, RegrasClassificacao.CRITERIOS_PADRAO);

        assertEquals(5, editado.configuracao().melhorDe());
        assertFalse(editado.congelado());
    }

    // ---- idempotência ----

    @Test
    void repetirAChaveNaoGravaDuasVezesEOutroCorpoEhRecusado() {
        UUID chave = UUID.randomUUID();
        AtomicInteger execucoes = new AtomicInteger();
        String escopo = idempotencia.escopo("PUT /teste", Map.of("a", 1));

        Map<?, ?> primeira = idempotencia.executar(chave, escopo, Map.class, () -> Map.of("resultado", execucoes.incrementAndGet()));
        Map<?, ?> segunda = idempotencia.executar(chave, escopo, Map.class, () -> Map.of("resultado", execucoes.incrementAndGet()));

        assertEquals(1, execucoes.get(), "a ação roda uma vez só");
        assertEquals(primeira, segunda, "a repetição devolve a mesma resposta");
        ErroDeRegra reutilizada = assertThrows(ErroDeRegra.class,
                () -> idempotencia.executar(chave, idempotencia.escopo("PUT /teste", Map.of("a", 2)), Map.class, Map::of));
        assertEquals("CHAVE_REUTILIZADA", reutilizada.codigo());
    }

    // ---- ciclo de imagem (AD-16, FR-26) ----

    @Test
    void aImagemEnviadaVoltaIgualDepoisDeDerrubarEReiniciarAAplicacao() throws Exception {
        var time = times.criar("Time Imagem " + CONTADOR.incrementAndGet(), "IMG" + CONTADOR.get());
        imagens.enviarLogo(null, time.id(), png(300, 200));
        String url = times.buscar(time.id()).logoUrl();
        assertNotNull(url, "o Time precisa ter a URL pública do logo");
        String chave = url.substring("http://armazenamento.teste/rks-img/".length());
        String hashAntes = sha256(ARMAZENAMENTO.get(chave));

        // Derruba e reinicia a aplicação (outro contexto, mesmo banco, mesmo armazenamento externo).
        try (ConfigurableApplicationContext reiniciada = new SpringApplicationBuilder(Application.class, ConfiguracaoDeTeste.class)
                .profiles("integracao")
                .properties(
                        "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRES.getUsername(),
                        "spring.datasource.password=" + POSTGRES.getPassword(),
                        "server.port=0")
                .run()) {
            String urlDepois = reiniciada.getBean(TimeService.class).buscar(time.id()).logoUrl();
            assertEquals(url, urlDepois, "a referência sobrevive ao reinício");
            assertEquals(hashAntes, sha256(ARMAZENAMENTO.get(chave)), "a mesma imagem volta, com hash igual");
        }
        assertFalse(new File("uploads").exists(), "nenhuma imagem pode ficar em uploads/ local");
    }

    @Test
    void formatoInvalidoNaoDeixaReferenciaNemObjeto() {
        var time = times.criar("Time Invalido " + CONTADOR.incrementAndGet(), "INV" + CONTADOR.get());
        int objetosAntes = ARMAZENAMENTO.size();

        ErroDeRegra erro = assertThrows(ErroDeRegra.class,
                () -> imagens.enviarLogo(null, time.id(), "isto não é imagem".getBytes(StandardCharsets.UTF_8)));

        assertEquals("IMAGEM_FORMATO_INVALIDO", erro.codigo());
        assertEquals(objetosAntes, ARMAZENAMENTO.size());
        assertEquals(null, times.buscar(time.id()).logoUrl());
    }

    // ---- apoio ----

    private record Cenario(UUID campeonato, UUID partida) {
    }

    /** Campeonato com dois Times prontos (Elenco salvo e vinculado) e uma Partida agendada. */
    private Cenario criarCenario(int melhorDe) {
        int n = CONTADOR.incrementAndGet();
        var temporada = temporadas.criar("Temporada " + n, null, null);
        var campeonato = campeonatos.criar(null, temporada.id(), "Campeonato " + n, null, melhorDe, null, null);
        List<UUID> idsDosTimes = new ArrayList<>();
        for (String lado : List.of("A", "B")) {
            var time = times.criar("Time " + lado + n, lado + n);
            List<ElencoService.MembroPedido> membros = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                var jogador = jogadores.criar("J" + lado + i + "x" + n, "BR1");
                membros.add(new ElencoService.MembroPedido(jogador.id(), "TITULAR", i == 0));
            }
            elencos.salvar(null, time.id(), temporada.id(), membros);
            elencos.vincular(null, campeonato.id(), time.id());
            idsDosTimes.add(time.id());
        }
        var partida = partidas.agendar(null, campeonato.faseId(), idsDosTimes.get(0), idsDosTimes.get(1), 1, null, null);
        return new Cenario(campeonato.id(), partida.id());
    }

    private static byte[] png(int largura, int altura) throws Exception {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return saida.toByteArray();
    }

    private static String sha256(byte[] dados) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados));
    }
}
