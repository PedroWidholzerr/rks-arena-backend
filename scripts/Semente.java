import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Semente de DEMONSTRAÇÃO para validar o layout: uma Temporada com um Campeonato de pontos corridos, 6 Times com Elenco
 * e Capitão, e 15 Partidas simuladas (resultados, W.O., um jogo ao vivo, jogos de hoje e futuros, e um empate real).
 *
 * Uso (com DATABASE_URL, DB_USER e DB_PASSWORD no ambiente):
 *   java -cp postgresql-42.7.10.jar scripts/Semente.java criar
 *   java -cp postgresql-42.7.10.jar scripts/Semente.java remover
 *
 * Tudo nasce ligado à Temporada "Temporada Demo", e "remover" apaga só o que está ligado a ela (Times e Jogadores
 * da semente inclusive). Nada é gravado em auditoria nem em outras Temporadas.
 */
public class Semente {

    static final String TEMPORADA = "Temporada Demo";

    record TimeDemo(String nome, String tag, String[] titulares, String[] reservas) {
    }

    static final TimeDemo[] TIMES = {
            new TimeDemo("Dragões de Aço", "DRA", new String[]{"Kael", "Mirela", "Thorin"}, new String[]{"Zefa"}),
            new TimeDemo("Lobos da Aurora", "LOB", new String[]{"Fenrir", "Aurea", "Bruno7"}, new String[]{"Nina", "Otto"}),
            new TimeDemo("Falcões Negros", "FAL", new String[]{"Ravena", "Dante", "Selene"}, new String[]{"Iago"}),
            new TimeDemo("Titãs do Sul", "TIT", new String[]{"Atlas", "Hera", "Caio"}, new String[]{}),
            new TimeDemo("Corvos Reais", "COR", new String[]{"Edgar", "Lyra", "Matias"}, new String[]{"Pri"}),
            new TimeDemo("Guardiões da Névoa", "GUA", new String[]{"Orion", "Vesta", "Luca"}, new String[]{"Tati", "Rui"}),
    };

    /** Rodada, posição, Time A, Time B, jogos A, jogos B, estado (F finalizada, W W.O., V ao vivo, A agendada), dias a partir de hoje. */
    static final Object[][] PARTIDAS = {
            // Rodada 1
            {1, 1, 0, 5, 2, 0, "F", -6}, {1, 2, 1, 4, 2, 0, "F", -6}, {1, 3, 2, 3, 2, 1, "F", -6},
            // Rodada 2 (um W.O.)
            {2, 1, 0, 4, 2, 1, "F", -4}, {2, 2, 5, 3, 2, 0, "W", -4}, {2, 3, 1, 2, 2, 1, "F", -4},
            // Rodada 3
            {3, 1, 0, 3, 2, 0, "F", -2}, {3, 2, 4, 2, 0, 2, "F", -2}, {3, 3, 5, 1, 0, 2, "F", -2},
            // Rodada 4
            {4, 1, 0, 2, 0, 2, "F", -1}, {4, 2, 3, 1, 2, 0, "F", -1}, {4, 3, 4, 5, 2, 1, "F", -1},
            // Rodada 5: 0 x 1 amanhã (decidiria o empate), 2 x 5 ao vivo agora, 3 x 4 hoje à noite
            {5, 1, 0, 1, 0, 0, "A", 1}, {5, 2, 2, 5, 0, 0, "V", 0}, {5, 3, 3, 4, 0, 0, "A", 0},
    };

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("criar") || args[0].equals("remover"))) {
            System.err.println("Uso: Semente.java criar|remover");
            System.exit(2);
        }
        String url = System.getenv("DATABASE_URL").replaceAll("&currentSchema=[^&]*", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))) {
            c.setAutoCommit(false);
            try {
                if (args[0].equals("criar")) criar(c); else remover(c);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    static void criar(Connection c) throws Exception {
        try (PreparedStatement q = c.prepareStatement("select count(*) from temporada where nome = ?")) {
            q.setString(1, TEMPORADA);
            ResultSet r = q.executeQuery();
            r.next();
            if (r.getInt(1) > 0) {
                System.out.println("A semente já existe (Temporada Demo). Rode 'remover' antes de criar de novo.");
                return;
            }
        }
        UUID temporada = UUID.randomUUID(), campeonato = UUID.randomUUID(), fase = UUID.randomUUID();
        exec(c, "insert into temporada (id, nome, data_inicio, encerrada) values (?, ?, current_date - 30, false)", temporada, TEMPORADA);
        exec(c, "insert into campeonato (id, temporada_id, nome, descricao, modo, tamanho_time, melhor_de, pontos_vitoria, pontos_derrota_decisiva, pontos_derrota, ordem_desempate, estado) "
                + "values (?, ?, 'Copa Demo RKS', 'Campeonato de demonstração para validar o layout. Pode ser removido sem deixar rastro.', 'ARAM', 3, 3, 2, 1, 0, 'CONFRONTO_DIRETO,SALDO,JOGOS_VENCIDOS', 'EM_ANDAMENTO')",
                campeonato, temporada);
        exec(c, "insert into fase (id, campeonato_id, nome, formato, ordem, turnos) values (?, ?, 'Pontos corridos', 'PONTOS_CORRIDOS', 1, 1)", fase, campeonato);

        UUID[] times = new UUID[TIMES.length];
        UUID[] capitaes = new UUID[TIMES.length];
        for (int t = 0; t < TIMES.length; t++) {
            TimeDemo td = TIMES[t];
            times[t] = UUID.randomUUID();
            UUID elenco = UUID.randomUUID();
            exec(c, "insert into \"time\" (id, nome, tag, tag_key) values (?, ?, ?, ?)", times[t], td.nome(), td.tag(), td.tag().toLowerCase());
            exec(c, "insert into elenco (id, time_id, temporada_id) values (?, ?, ?)", elenco, times[t], temporada);
            List<String> todos = new ArrayList<>(List.of(td.titulares()));
            todos.addAll(List.of(td.reservas()));
            for (int j = 0; j < todos.size(); j++) {
                UUID jogador = UUID.randomUUID();
                String nick = todos.get(j);
                exec(c, "insert into jogador (id, nick, tag, riot_id_key) values (?, ?, 'BR1', ?)", jogador, nick, nick.toLowerCase() + "#br1");
                boolean titular = j < td.titulares().length;
                exec(c, "insert into elenco_membro (elenco_id, jogador_id, funcao, capitao) values (?, ?, ?, ?)",
                        elenco, jogador, titular ? "TITULAR" : "RESERVA", j == 0);
                exec(c, "insert into campeonato_jogador (campeonato_id, jogador_id) values (?, ?)", campeonato, jogador);
                if (j == 0) capitaes[t] = jogador;
            }
            exec(c, "insert into participacao_campeonato (id, campeonato_id, time_id, elenco_id) values (?, ?, ?, ?)", UUID.randomUUID(), campeonato, times[t], elenco);
        }

        Instant agora = Instant.now();
        for (Object[] p : PARTIDAS) {
            int rodada = (int) p[0], posicao = (int) p[1], a = (int) p[2], b = (int) p[3], ja = (int) p[4], jb = (int) p[5];
            String tipo = (String) p[6];
            int dias = (int) p[7];
            Instant quando = switch (tipo) {
                case "V" -> agora.minus(Duration.ofMinutes(35));
                case "A" -> dias == 0 ? agora.plus(Duration.ofHours(3)) : agora.plus(Duration.ofDays(dias)).plus(Duration.ofHours(2));
                default -> agora.plus(Duration.ofDays(dias)).minus(Duration.ofHours(3));
            };
            boolean finalizada = tipo.equals("F") || tipo.equals("W");
            UUID mvp = tipo.equals("F") ? capitaes[ja > jb ? a : b] : null;
            String estado = finalizada ? "FINALIZADA" : tipo.equals("V") ? "EM_ANDAMENTO" : "AGENDADA";
            try (PreparedStatement s = c.prepareStatement(
                    "insert into partida (id, fase_id, rodada, posicao, time_a_id, time_b_id, estado, placar_a, placar_b, walkover, agendada_para, stream_url, mvp_jogador_id) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                s.setObject(1, UUID.randomUUID());
                s.setObject(2, fase);
                s.setShort(3, (short) rodada);
                s.setShort(4, (short) posicao);
                s.setObject(5, times[a]);
                s.setObject(6, times[b]);
                s.setString(7, estado);
                s.setObject(8, finalizada ? (Short) (short) ja : null);
                s.setObject(9, finalizada ? (Short) (short) jb : null);
                s.setBoolean(10, tipo.equals("W"));
                s.setTimestamp(11, Timestamp.from(quando));
                s.setString(12, tipo.equals("V") ? "https://www.twitch.tv/riotgames" : null);
                s.setObject(13, mvp);
                s.executeUpdate();
            }
        }
        System.out.println("Semente criada: Temporada Demo, Copa Demo RKS, 6 Times, " + TIMES.length * 4 + " Jogadores (aprox.) e " + PARTIDAS.length + " Partidas.");
        System.out.println("Para remover: java -cp <postgresql.jar> scripts/Semente.java remover");
    }

    static void remover(Connection c) throws Exception {
        List<UUID> times = ids(c, "select p.time_id from participacao_campeonato p join campeonato c on c.id = p.campeonato_id join temporada t on t.id = c.temporada_id where t.nome = ?");
        List<UUID> jogadores = ids(c, "select m.jogador_id from elenco_membro m join elenco e on e.id = m.elenco_id join temporada t on t.id = e.temporada_id where t.nome = ?");
        String porTemporada = "(select id from temporada where nome = ?)";
        String fases = "(select f.id from fase f join campeonato c on c.id = f.campeonato_id where c.temporada_id in " + porTemporada + ")";
        String campeonatos = "(select id from campeonato where temporada_id in " + porTemporada + ")";
        int n = 0;
        n += exec(c, "delete from partida where fase_id in " + fases, TEMPORADA);
        n += exec(c, "delete from decisao_desempate where fase_id in " + fases, TEMPORADA);
        n += exec(c, "delete from participacao_campeonato where campeonato_id in " + campeonatos, TEMPORADA);
        n += exec(c, "delete from campeonato_jogador where campeonato_id in " + campeonatos, TEMPORADA);
        n += exec(c, "delete from elenco_membro where elenco_id in (select id from elenco where temporada_id in " + porTemporada + ")", TEMPORADA);
        n += exec(c, "delete from elenco where temporada_id in " + porTemporada, TEMPORADA);
        n += exec(c, "delete from fase where campeonato_id in " + campeonatos, TEMPORADA);
        n += exec(c, "delete from campeonato where temporada_id in " + porTemporada, TEMPORADA);
        n += exec(c, "delete from temporada where nome = ?", TEMPORADA);
        for (UUID j : jogadores) n += exec(c, "delete from jogador where id = ?", j);
        for (UUID t : times) n += exec(c, "delete from \"time\" where id = ?", t);
        System.out.println("Semente removida (" + n + " linhas).");
    }

    static List<UUID> ids(Connection c, String sql) throws Exception {
        List<UUID> lista = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, TEMPORADA);
            ResultSet r = s.executeQuery();
            while (r.next()) lista.add((UUID) r.getObject(1));
        }
        return lista;
    }

    static int exec(Connection c, String sql, Object... params) throws Exception {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) s.setObject(i + 1, params[i]);
            return s.executeUpdate();
        }
    }
}
