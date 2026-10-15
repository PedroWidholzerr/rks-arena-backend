package com.rks.integracao;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aplica a V1 num Postgres limpo e prova cada CHECK/índice com um insert inválido.
 * Roda só no perfil "integracao" (precisa de Docker).
 */
@Tag("integration")
@Testcontainers
class MigracaoV1IntegracaoTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private static final AtomicInteger CONTADOR = new AtomicInteger(100);
    private static final String TEMPORADA = UUID.randomUUID().toString();
    private static final String CAMPEONATO = UUID.randomUUID().toString();
    private static final String FASE = UUID.randomUUID().toString();
    private static final String TIME_A = UUID.randomUUID().toString();
    private static final String TIME_B = UUID.randomUUID().toString();
    private static final String ELENCO = UUID.randomUUID().toString();
    private static final String JOGADOR_1 = UUID.randomUUID().toString();
    private static final String JOGADOR_2 = UUID.randomUUID().toString();

    @BeforeAll
    static void aplicaV1ECriaBase() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        executar("INSERT INTO temporada (id, nome) VALUES ('" + TEMPORADA + "', 'T1')");
        executar("INSERT INTO campeonato (id, temporada_id, nome) VALUES ('" + CAMPEONATO + "', '" + TEMPORADA + "', 'C1')");
        executar("INSERT INTO fase (id, campeonato_id, nome) VALUES ('" + FASE + "', '" + CAMPEONATO + "', 'Pontos corridos')");
        executar("INSERT INTO \"time\" (id, nome, tag, tag_key) VALUES ('" + TIME_A + "', 'Time A', 'AAA', 'aaa')");
        executar("INSERT INTO \"time\" (id, nome, tag, tag_key) VALUES ('" + TIME_B + "', 'Time B', 'BBB', 'bbb')");
        executar("INSERT INTO elenco (id, time_id, temporada_id) VALUES ('" + ELENCO + "', '" + TIME_A + "', '" + TEMPORADA + "')");
        executar("INSERT INTO jogador (id, nick, tag, riot_id_key) VALUES ('" + JOGADOR_1 + "', 'Um', 'BR1', 'um#br1')");
        executar("INSERT INTO jogador (id, nick, tag, riot_id_key) VALUES ('" + JOGADOR_2 + "', 'Dois', 'BR1', 'dois#br1')");
    }

    @Test
    void v1CriaTodasAsTabelas() throws SQLException {
        for (String tabela : new String[]{"usuario", "temporada", "campeonato", "fase", "time", "jogador", "elenco",
                "elenco_membro", "participacao_campeonato", "campeonato_jogador", "partida", "decisao_desempate",
                "chave_idempotencia", "audit_log"}) {
            try (Connection c = conexao(); Statement s = c.createStatement()) {
                s.execute("SELECT 1 FROM \"" + tabela + "\" LIMIT 1");
            }
        }
    }

    @Test
    void placarNuloForaDeFinalizada() {
        violacao("ck_partida_placar_estado", partida("AGENDADA", "1", "0"));
        violacao("ck_partida_placar_estado", partida("EM_ANDAMENTO", "1", "0"));
        violacao("ck_partida_placar_estado", partida("FINALIZADA", "NULL", "NULL"));
        violacao("ck_partida_placar_estado", partida("FINALIZADA", "1", "NULL"));
    }

    @Test
    void aceitaPartidaAnuladaEFinalizadaValida() throws SQLException {
        executar(partida("ANULADA", "NULL", "NULL"));
        executar(partida("FINALIZADA", "2", "0"));
    }

    @Test
    void placarNaoPodeSerNegativo() {
        violacao("ck_partida_placar_nao_negativo", partida("FINALIZADA", "-1", "2"));
    }

    @Test
    void timesDaPartidaDevemSerDistintos() {
        violacao("ck_partida_times_distintos",
                "INSERT INTO partida (fase_id, rodada, posicao, time_a_id, time_b_id) VALUES ('"
                        + FASE + "', 90, 1, '" + TIME_A + "', '" + TIME_A + "')");
    }

    @Test
    void walkoverExigeFinalizada() {
        violacao("ck_partida_walkover",
                "INSERT INTO partida (fase_id, rodada, posicao, walkover) VALUES ('" + FASE + "', 91, 1, true)");
    }

    @Test
    void estadoDePartidaInvalido() {
        violacao("ck_partida_estado", partida("QUALQUER", "NULL", "NULL"));
    }

    @Test
    void partidaTemColunaVersionEStreamUrl() throws SQLException {
        try (Connection c = conexao(); Statement s = c.createStatement()) {
            s.execute("SELECT version, stream_url FROM partida LIMIT 1");
        }
    }

    @Test
    void umCapitaoPorElenco() throws SQLException {
        executar("INSERT INTO elenco_membro (elenco_id, jogador_id, capitao) VALUES ('" + ELENCO + "', '" + JOGADOR_1 + "', true)");
        violacao("ux_elenco_membro_um_capitao",
                "INSERT INTO elenco_membro (elenco_id, jogador_id, capitao) VALUES ('" + ELENCO + "', '" + JOGADOR_2 + "', true)");
        executar("INSERT INTO elenco_membro (elenco_id, jogador_id, capitao) VALUES ('" + ELENCO + "', '" + JOGADOR_2 + "', false)");
    }

    @Test
    void tagKeyEUnica() {
        violacao("ux_time_tag_key",
                "INSERT INTO \"time\" (nome, tag, tag_key) VALUES ('Outro', 'AAA', 'aaa')");
    }

    @Test
    void riotIdKeyEUnico() {
        violacao("ux_jogador_riot_id_key",
                "INSERT INTO jogador (nick, tag, riot_id_key) VALUES ('Um', 'BR1', 'um#br1')");
    }

    @Test
    void chavesPrecisamEstarEmMinuscula() {
        violacao("ck_time_tag_key", "INSERT INTO \"time\" (nome, tag, tag_key) VALUES ('X', 'XYZ', 'XYZ')");
        violacao("ck_jogador_riot_id_key", "INSERT INTO jogador (nick, tag, riot_id_key) VALUES ('X', 'BR1', 'X#BR1')");
    }

    @Test
    void jogadorEUnicoPorCampeonato() throws SQLException {
        executar("INSERT INTO campeonato_jogador (campeonato_id, jogador_id) VALUES ('" + CAMPEONATO + "', '" + JOGADOR_1 + "')");
        violacao("campeonato_jogador_pkey",
                "INSERT INTO campeonato_jogador (campeonato_id, jogador_id) VALUES ('" + CAMPEONATO + "', '" + JOGADOR_1 + "')");
    }

    @Test
    void melhorDeDeveSerImpar() {
        violacao("ck_campeonato_melhor_de", campeonatoComMelhorDe(2));
        violacao("ck_campeonato_melhor_de", campeonatoComMelhorDe(0));
    }

    @Test
    void pontuacaoExigeVitoriaMaiorQueDerrotaDecisivaMaiorOuIgualADerrota() {
        violacao("ck_campeonato_pontuacao",
                "INSERT INTO campeonato (temporada_id, nome, pontos_vitoria, pontos_derrota_decisiva, pontos_derrota) VALUES ('"
                        + TEMPORADA + "', 'P1', 1, 1, 0)");
        violacao("ck_campeonato_pontuacao",
                "INSERT INTO campeonato (temporada_id, nome, pontos_vitoria, pontos_derrota_decisiva, pontos_derrota) VALUES ('"
                        + TEMPORADA + "', 'P2', 3, 0, 1)");
        violacao("ck_campeonato_pontuacao",
                "INSERT INTO campeonato (temporada_id, nome, pontos_vitoria, pontos_derrota_decisiva, pontos_derrota) VALUES ('"
                        + TEMPORADA + "', 'P3', 3, 0, -1)");
    }

    @Test
    void usuarioSoAceitaPapelAdmin() {
        violacao("ck_usuario_papel",
                "INSERT INTO usuario (email, senha_hash, papel) VALUES ('a@a.com', 'x', 'JOGADOR')");
    }

    @Test
    void decisaoDesempateExigeMotivoEConjuntoValido() {
        String t1 = UUID.randomUUID().toString();
        String t2 = UUID.randomUUID().toString();
        violacao("ck_decisao_desempate_motivo",
                "INSERT INTO decisao_desempate (fase_id, times_ids, ordem_ids, motivo) VALUES ('" + FASE
                        + "', ARRAY['" + t1 + "','" + t2 + "']::uuid[], ARRAY['" + t1 + "','" + t2 + "']::uuid[], '  ')");
        violacao("ck_decisao_desempate_tamanho",
                "INSERT INTO decisao_desempate (fase_id, times_ids, ordem_ids, motivo) VALUES ('" + FASE
                        + "', ARRAY['" + t1 + "','" + t2 + "']::uuid[], ARRAY['" + t1 + "']::uuid[], 'sorteio')");
    }

    @Test
    void elencoEUnicoPorTimeETemporada() {
        violacao("ux_elenco_time_temporada",
                "INSERT INTO elenco (time_id, temporada_id) VALUES ('" + TIME_A + "', '" + TEMPORADA + "')");
    }

    @Test
    void idsSaoUuidEInstantesTemFusoEPlacarESmallint() throws SQLException {
        try (Connection c = conexao(); Statement s = c.createStatement()) {
            var rs = s.executeQuery("SELECT table_name, column_name, data_type FROM information_schema.columns "
                    + "WHERE table_schema = 'public' AND ((column_name = 'id' AND data_type <> 'uuid') "
                    + "OR (column_name IN ('criado_em', 'atualizado_em', 'em', 'agendada_para') AND data_type <> 'timestamp with time zone') "
                    + "OR (column_name IN ('placar_a', 'placar_b') AND data_type <> 'smallint'))");
            assertTrue(!rs.next(), "Coluna com tipo fora da convenção: " + (rs.getRow() > 0 ? rs.getString(1) + "." + rs.getString(2) : ""));
        }
    }

    // --- utilitários ---

    private static String partida(String estado, String placarA, String placarB) {
        return "INSERT INTO partida (fase_id, rodada, posicao, estado, placar_a, placar_b) VALUES ('" + FASE + "', "
                + CONTADOR.incrementAndGet() + ", 1, '" + estado + "', "
                + placarA + ", " + placarB + ")";
    }

    private static String campeonatoComMelhorDe(int melhorDe) {
        return "INSERT INTO campeonato (temporada_id, nome, melhor_de) VALUES ('" + TEMPORADA + "', 'M" + melhorDe + "', " + melhorDe + ")";
    }

    private static Connection conexao() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void executar(String sql) throws SQLException {
        try (Connection c = conexao(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private static void violacao(String restricao, String sql) {
        SQLException erro = assertThrows(SQLException.class, () -> executar(sql), "Era esperada violação de " + restricao);
        assertTrue(erro.getSQLState().startsWith("23"), "SQLState inesperado: " + erro.getSQLState());
        assertTrue(erro.getMessage().contains(restricao),
                "Esperava a restrição '" + restricao + "' na mensagem: " + erro.getMessage());
        assertEquals("23", erro.getSQLState().substring(0, 2));
    }
}
