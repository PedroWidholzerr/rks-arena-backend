package com.rks.domain.standings;

import com.rks.domain.rules.EstadoPartida;
import com.rks.domain.rules.RegrasDePlacar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Única origem de qualquer número de tabela (AD-2). Função pura: sem relógio, sem acesso a dados
 * e sem estado guardado. A tabela é recalculada a partir das Partidas FINALIZADAS a cada consulta;
 * nenhum código soma ou subtrai pontos de uma tabela existente.
 */
public final class Standings {

    private Standings() {
    }

    /** Bloco de Times consecutivos na tabela; {@code empatado} marca os que nenhum critério separou. */
    private record Bloco(List<UUID> times, boolean empatado) {
    }

    /** Acumulado de um Time sobre as Partidas FINALIZADAS consideradas. */
    private static final class Acumulado {
        int jogadas;
        int vitorias;
        int derrotas;
        int jogosAFavor;
        int jogosContra;
        int pontos;

        int saldo() {
            return jogosAFavor - jogosContra;
        }
    }

    public static Classificacao compute(
            Collection<UUID> participantes,
            Collection<PartidaResultado> partidas,
            RegrasClassificacao regras,
            Collection<DecisaoManual> decisoesManuais) {

        Set<UUID> times = new LinkedHashSet<>(participantes);
        Map<UUID, Acumulado> tabela = acumular(times, partidas, regras);

        // Critério 1: pontos. Cada grupo de mesma pontuação é resolvido pelos demais critérios.
        TreeMap<Integer, List<UUID>> porPontos = new TreeMap<>(Comparator.reverseOrder());
        for (UUID time : times) {
            porPontos.computeIfAbsent(tabela.get(time).pontos, p -> new ArrayList<>()).add(time);
        }
        List<Bloco> blocos = new ArrayList<>();
        for (List<UUID> grupo : porPontos.values()) {
            blocos.addAll(resolver(grupo, partidas, regras, tabela));
        }

        // Critério 5: decisão manual, só para o conjunto exato de empatados.
        List<DecisaoManual> decisoes = new ArrayList<>(decisoesManuais);
        boolean[] usada = new boolean[decisoes.size()];
        List<Bloco> aposDecisao = new ArrayList<>();
        for (Bloco bloco : blocos) {
            int indice = bloco.empatado() ? decisaoParaConjunto(decisoes, usada, bloco.times()) : -1;
            if (indice < 0) {
                aposDecisao.add(bloco);
            } else {
                usada[indice] = true;
                for (UUID time : decisoes.get(indice).ordem()) {
                    aposDecisao.add(new Bloco(List.of(time), false));
                }
            }
        }

        List<Linha> linhas = new ArrayList<>();
        int proximaPosicao = 1;
        for (Bloco bloco : aposDecisao) {
            for (UUID time : bloco.times()) {
                Acumulado a = tabela.get(time);
                linhas.add(new Linha(proximaPosicao, time, a.jogadas, a.vitorias, a.derrotas,
                        a.jogosAFavor, a.jogosContra, a.saldo(), a.pontos, bloco.empatado()));
            }
            proximaPosicao += bloco.times().size();
        }

        List<DecisaoManual> orfas = new ArrayList<>();
        for (int i = 0; i < decisoes.size(); i++) {
            if (!usada[i]) {
                orfas.add(decisoes.get(i));
            }
        }
        return new Classificacao(linhas, orfas);
    }

    /** Índice da primeira decisão ainda não usada cujo conjunto é igual ao do bloco, ou -1. */
    private static int decisaoParaConjunto(List<DecisaoManual> decisoes, boolean[] usada, List<UUID> bloco) {
        Set<UUID> conjunto = new LinkedHashSet<>(bloco);
        for (int i = 0; i < decisoes.size(); i++) {
            if (!usada[i] && decisoes.get(i).conjunto().equals(conjunto)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Aplica os critérios da lista, na ordem, a um grupo com a mesma pontuação. O primeiro que
     * separar o grupo o parte; cada subgrupo restante recomeça pelo primeiro critério da lista.
     * Sem critério que separe, o grupo sai empatado, em ordem estável por identificador.
     */
    private static List<Bloco> resolver(
            List<UUID> grupo,
            Collection<PartidaResultado> partidas,
            RegrasClassificacao regras,
            Map<UUID, Acumulado> tabela) {

        if (grupo.size() == 1) {
            return List.of(new Bloco(grupo, false));
        }
        for (CriterioDesempate criterio : regras.criterios()) {
            Map<UUID, Integer> chaves = chavesDoCriterio(criterio, grupo, partidas, regras, tabela);
            TreeMap<Integer, List<UUID>> subgrupos = new TreeMap<>(Comparator.reverseOrder());
            for (UUID time : grupo) {
                subgrupos.computeIfAbsent(chaves.get(time), k -> new ArrayList<>()).add(time);
            }
            if (subgrupos.size() > 1) {
                List<Bloco> resultado = new ArrayList<>();
                for (List<UUID> subgrupo : subgrupos.values()) {
                    resultado.addAll(resolver(subgrupo, partidas, regras, tabela));
                }
                return resultado;
            }
        }
        List<UUID> ordenado = new ArrayList<>(grupo);
        ordenado.sort(Comparator.naturalOrder());
        return List.of(new Bloco(ordenado, true));
    }

    private static Map<UUID, Integer> chavesDoCriterio(
            CriterioDesempate criterio,
            List<UUID> grupo,
            Collection<PartidaResultado> partidas,
            RegrasClassificacao regras,
            Map<UUID, Acumulado> tabela) {

        Map<UUID, Integer> chaves = new HashMap<>();
        switch (criterio) {
            case CONFRONTO_DIRETO -> {
                // Mini-tabela só com as Partidas entre os empatados, pelas mesmas regras de pontos.
                Map<UUID, Acumulado> mini = acumular(new LinkedHashSet<>(grupo), partidas, regras);
                for (UUID time : grupo) {
                    chaves.put(time, mini.get(time).pontos);
                }
            }
            case SALDO -> grupo.forEach(time -> chaves.put(time, tabela.get(time).saldo()));
            case JOGOS_VENCIDOS -> grupo.forEach(time -> chaves.put(time, tabela.get(time).jogosAFavor));
        }
        return chaves;
    }

    /**
     * Soma as Partidas FINALIZADAS entre Times do conjunto. ANULADA, AGENDADA e EM_ANDAMENTO ficam
     * fora, assim como placar sem vencedor (que a validação do AD-3 já impede de existir).
     */
    private static Map<UUID, Acumulado> acumular(
            Set<UUID> times, Collection<PartidaResultado> partidas, RegrasClassificacao regras) {

        Map<UUID, Acumulado> tabela = new HashMap<>();
        for (UUID time : times) {
            tabela.put(time, new Acumulado());
        }
        for (PartidaResultado p : partidas) {
            if (p.estado() != EstadoPartida.FINALIZADA || p.jogosA() == p.jogosB()
                    || !tabela.containsKey(p.timeA()) || !tabela.containsKey(p.timeB())) {
                continue;
            }
            boolean decisiva = RegrasDePlacar.ehDecisiva(regras.melhorDe(), p.jogosA(), p.jogosB(), p.walkover());
            int pontosDerrota = decisiva ? regras.pontuacao().derrotaDecisiva() : regras.pontuacao().derrota();
            boolean aVenceu = p.jogosA() > p.jogosB();

            Acumulado a = tabela.get(p.timeA());
            Acumulado b = tabela.get(p.timeB());
            a.jogadas++;
            b.jogadas++;
            a.jogosAFavor += p.jogosA();
            a.jogosContra += p.jogosB();
            b.jogosAFavor += p.jogosB();
            b.jogosContra += p.jogosA();
            Acumulado vencedor = aVenceu ? a : b;
            Acumulado perdedor = aVenceu ? b : a;
            vencedor.vitorias++;
            vencedor.pontos += regras.pontuacao().vitoria();
            perdedor.derrotas++;
            perdedor.pontos += pontosDerrota;
        }
        return tabela;
    }
}
