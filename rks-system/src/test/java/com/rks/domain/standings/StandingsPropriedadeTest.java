package com.rks.domain.standings;

import com.rks.domain.rules.EstadoPartida;
import com.rks.domain.rules.Pontuacao;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste de propriedade do AD-5: 1000 sementes de sequências de lançar, editar e anular. Em cada
 * passo compara o sistema com {@link StandingsReference} e confere idempotência, reversibilidade,
 * "anular = nunca ter existido" e invariância à ordem da entrada. A semente vai na mensagem de falha.
 */
class StandingsPropriedadeTest {

    private static final int SEMENTES = 1000;

    static Stream<Long> sementes() {
        return LongStream.range(0, SEMENTES).boxed();
    }

    /** Estado mutável de uma Partida durante a simulação. */
    private static final class Estado {
        final int a;
        final int b;
        EstadoPartida estado = EstadoPartida.AGENDADA;
        int jogosA;
        int jogosB;
        boolean walkover;

        Estado(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    /** Parâmetros sorteados da semente. */
    private record Cenario(int nTimes, int melhorDe, Pontuacao pontuacao, List<CriterioDesempate> criterios,
                           int[] ordemReferencia) {
    }

    @ParameterizedTest(name = "semente {0}")
    @MethodSource("sementes")
    void sistemaIgualReferenciaEPropriedadesValem(long semente) {
        Random sorteio = new Random(semente);
        Cenario cenario = sortearCenario(sorteio);

        List<Estado> partidas = new ArrayList<>();
        for (int a = 0; a < cenario.nTimes(); a++) {
            for (int b = a + 1; b < cenario.nTimes(); b++) {
                partidas.add(sorteio.nextBoolean() ? new Estado(a, b) : new Estado(b, a));
            }
        }

        int operacoes = 5 + sorteio.nextInt(40);
        for (int passo = 0; passo < operacoes; passo++) {
            aplicarOperacaoAleatoria(sorteio, partidas, cenario);
            verificar(semente, passo, sorteio, partidas, cenario);
        }
    }

    private static Cenario sortearCenario(Random sorteio) {
        int nTimes = 3 + sorteio.nextInt(4);
        int[] melhores = {1, 3, 5};
        int melhorDe = melhores[sorteio.nextInt(melhores.length)];
        Pontuacao pontuacao = sorteio.nextBoolean()
                ? Pontuacao.PADRAO
                : new Pontuacao(3, 1, sorteio.nextInt(2));
        List<CriterioDesempate> criterios = new ArrayList<>(RegrasClassificacao.CRITERIOS_PADRAO);
        Collections.shuffle(criterios, sorteio);
        int[] ordem = new int[criterios.size()];
        for (int i = 0; i < ordem.length; i++) {
            ordem[i] = switch (criterios.get(i)) {
                case CONFRONTO_DIRETO -> 0;
                case SALDO -> 1;
                case JOGOS_VENCIDOS -> 2;
            };
        }
        return new Cenario(nTimes, melhorDe, pontuacao, criterios, ordem);
    }

    private static void aplicarOperacaoAleatoria(Random sorteio, List<Estado> partidas, Cenario cenario) {
        Estado alvo = partidas.get(sorteio.nextInt(partidas.size()));
        int operacao = sorteio.nextInt(3);
        if (alvo.estado == EstadoPartida.FINALIZADA && operacao == 2) {
            // anular resultado: volta a AGENDADA (ou ANULADA) sem placar
            alvo.estado = sorteio.nextBoolean() ? EstadoPartida.AGENDADA : EstadoPartida.ANULADA;
            alvo.jogosA = 0;
            alvo.jogosB = 0;
            alvo.walkover = false;
        } else if (alvo.estado == EstadoPartida.FINALIZADA) {
            lancar(sorteio, alvo, cenario.melhorDe()); // editar
        } else if (alvo.estado == EstadoPartida.AGENDADA && operacao != 2) {
            lancar(sorteio, alvo, cenario.melhorDe()); // lançar
        } else if (alvo.estado == EstadoPartida.AGENDADA) {
            alvo.estado = EstadoPartida.EM_ANDAMENTO;
        } else {
            alvo.estado = EstadoPartida.AGENDADA;
        }
    }

    /** Grava um placar válido (⌈N/2⌉ para o vencedor, menos para o perdedor) ou um W.O. */
    private static void lancar(Random sorteio, Estado partida, int melhorDe) {
        int necessarios = (melhorDe + 1) / 2;
        boolean aVence = sorteio.nextBoolean();
        boolean wo = sorteio.nextInt(8) == 0;
        int perdedor = wo ? 0 : sorteio.nextInt(necessarios);
        partida.estado = EstadoPartida.FINALIZADA;
        partida.walkover = wo;
        partida.jogosA = aVence ? necessarios : perdedor;
        partida.jogosB = aVence ? perdedor : necessarios;
    }

    private static void verificar(long semente, int passo, Random sorteio, List<Estado> partidas, Cenario cenario) {
        String onde = "semente=" + semente + " passo=" + passo;
        List<UUID> times = times(cenario.nTimes());
        RegrasClassificacao regras = new RegrasClassificacao(cenario.pontuacao(), cenario.melhorDe(), cenario.criterios());

        Classificacao sistema = Standings.compute(times, resultados(partidas), regras, List.of());

        // (a) sistema == referência
        assertEquals(paraComparacao(referencia(cenario, partidas)), paraComparacao(sistema),
                onde + " (sistema diferente da referência)");

        // (b) idempotência
        assertEquals(sistema, Standings.compute(times, resultados(partidas), regras, List.of()),
                onde + " (idempotência)");

        // (e) permutar a entrada não muda a saída
        List<PartidaResultado> embaralhadas = new ArrayList<>(resultados(partidas));
        Collections.shuffle(embaralhadas, sorteio);
        List<UUID> timesEmbaralhados = new ArrayList<>(times);
        Collections.shuffle(timesEmbaralhados, sorteio);
        assertEquals(sistema, Standings.compute(timesEmbaralhados, embaralhadas, regras, List.of()),
                onde + " (permutação da entrada)");

        Estado escolhida = partidas.get(sorteio.nextInt(partidas.size()));
        if (escolhida.estado == EstadoPartida.FINALIZADA) {
            // (c) reversibilidade: editar e voltar ao original devolve a tabela original
            int jogosA = escolhida.jogosA;
            int jogosB = escolhida.jogosB;
            boolean wo = escolhida.walkover;
            lancar(sorteio, escolhida, cenario.melhorDe());
            Standings.compute(times, resultados(partidas), regras, List.of());
            escolhida.jogosA = jogosA;
            escolhida.jogosB = jogosB;
            escolhida.walkover = wo;
            assertEquals(sistema, Standings.compute(times, resultados(partidas), regras, List.of()),
                    onde + " (reversibilidade)");

            // (d) anular equivale a nunca ter existido: sem a Partida na lista, o resultado é o mesmo
            List<Estado> semEla = new ArrayList<>(partidas);
            semEla.remove(escolhida);
            escolhida.estado = EstadoPartida.ANULADA;
            Classificacao anulada = Standings.compute(times, resultados(partidas), regras, List.of());
            Classificacao nuncaExistiu = Standings.compute(times, resultados(semEla), regras, List.of());
            escolhida.estado = EstadoPartida.FINALIZADA;
            assertEquals(nuncaExistiu, anulada, onde + " (anular = nunca ter existido)");
        }
    }

    private static List<UUID> times(int n) {
        List<UUID> times = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            times.add(new UUID(0L, i + 1));
        }
        return times;
    }

    private static List<PartidaResultado> resultados(List<Estado> partidas) {
        List<PartidaResultado> lista = new ArrayList<>();
        for (Estado p : partidas) {
            lista.add(new PartidaResultado(new UUID(0L, p.a + 1), new UUID(0L, p.b + 1),
                    p.jogosA, p.jogosB, p.walkover, p.estado, 1));
        }
        return lista;
    }

    private static List<StandingsReference.Posicao> referencia(Cenario cenario, List<Estado> partidas) {
        List<StandingsReference.Jogo> jogos = new ArrayList<>();
        for (Estado p : partidas) {
            jogos.add(new StandingsReference.Jogo(p.a, p.b, p.jogosA, p.jogosB, p.walkover,
                    p.estado == EstadoPartida.FINALIZADA));
        }
        Pontuacao pt = cenario.pontuacao();
        return StandingsReference.calcular(cenario.nTimes(), jogos, cenario.melhorDe(),
                pt.vitoria(), pt.derrotaDecisiva(), pt.derrota(), cenario.ordemReferencia());
    }

    /** Reduz as duas saídas a uma forma comum, em texto, para comparar tudo de uma vez. */
    private static List<String> paraComparacao(List<StandingsReference.Posicao> referencia) {
        List<String> saida = new ArrayList<>();
        for (StandingsReference.Posicao p : referencia) {
            saida.add(linha(p.time(), p.posicao(), p.empate(), p.jogadas(), p.vitorias(), p.derrotas(),
                    p.jogosAFavor(), p.jogosContra(), p.pontos()));
        }
        return saida;
    }

    private static List<String> paraComparacao(Classificacao classificacao) {
        List<String> saida = new ArrayList<>();
        for (Linha l : classificacao.linhas()) {
            saida.add(linha((int) l.timeId().getLeastSignificantBits() - 1, l.posicao(), l.empate(), l.jogadas(),
                    l.vitorias(), l.derrotas(), l.jogosAFavor(), l.jogosContra(), l.pontos()));
        }
        return saida;
    }

    private static String linha(int time, int posicao, boolean empate, int jogadas, int vitorias, int derrotas,
                                int favor, int contra, int pontos) {
        return "time=" + time + " pos=" + posicao + " empate=" + empate + " J=" + jogadas + " V=" + vitorias
                + " D=" + derrotas + " JF=" + favor + " JC=" + contra + " P=" + pontos;
    }
}
