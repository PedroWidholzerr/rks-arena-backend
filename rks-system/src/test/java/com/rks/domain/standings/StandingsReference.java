package com.rks.domain.standings;

import java.util.ArrayList;
import java.util.List;

/**
 * Referência independente da classificação (AD-5), escrita à parte e de propósito diferente de
 * {@link Standings}: Times são inteiros 0..n-1, tudo em laços explícitos sobre vetores, e o
 * desempate é uma fila de blocos que se partem até não haver mais o que separar. Não reutiliza
 * nenhuma classe de {@code com.rks.domain.standings}.
 */
final class StandingsReference {

    /** Partida da referência. {@code finalizada=false} cobre AGENDADA, EM_ANDAMENTO e ANULADA. */
    record Jogo(int a, int b, int jogosA, int jogosB, boolean walkover, boolean finalizada) {
    }

    /** Linha da referência, na ordem da tabela. */
    record Posicao(int time, int posicao, boolean empate, int jogadas, int vitorias, int derrotas,
                   int jogosAFavor, int jogosContra, int pontos) {
    }

    /** Critérios: 0 = confronto direto, 1 = saldo, 2 = Jogos vencidos. */
    static List<Posicao> calcular(int nTimes, List<Jogo> jogos, int melhorDe,
                                  int pontosVitoria, int pontosDerrotaDecisiva, int pontosDerrota,
                                  int[] ordemCriterios) {

        int[] jogadas = new int[nTimes];
        int[] vitorias = new int[nTimes];
        int[] derrotas = new int[nTimes];
        int[] favor = new int[nTimes];
        int[] contra = new int[nTimes];
        int[] pontos = new int[nTimes];

        for (Jogo j : jogos) {
            if (!j.finalizada()) {
                continue;
            }
            int vencedor = j.jogosA() > j.jogosB() ? j.a() : j.b();
            int perdedor = vencedor == j.a() ? j.b() : j.a();
            boolean decisiva = !j.walkover() && melhorDe > 1 && j.jogosA() + j.jogosB() == melhorDe;
            jogadas[j.a()] += 1;
            jogadas[j.b()] += 1;
            favor[j.a()] += j.jogosA();
            contra[j.a()] += j.jogosB();
            favor[j.b()] += j.jogosB();
            contra[j.b()] += j.jogosA();
            vitorias[vencedor] += 1;
            derrotas[perdedor] += 1;
            pontos[vencedor] += pontosVitoria;
            pontos[perdedor] += decisiva ? pontosDerrotaDecisiva : pontosDerrota;
        }

        // Fila de blocos. Começa com todos os Times em ordem decrescente de pontos, agrupados.
        List<List<Integer>> blocos = new ArrayList<>();
        List<Boolean> definitivo = new ArrayList<>();
        List<Integer> restantes = new ArrayList<>();
        for (int t = 0; t < nTimes; t++) {
            restantes.add(t);
        }
        while (!restantes.isEmpty()) {
            int melhor = Integer.MIN_VALUE;
            for (int t : restantes) {
                if (pontos[t] > melhor) {
                    melhor = pontos[t];
                }
            }
            List<Integer> grupo = new ArrayList<>();
            for (int t : restantes) {
                if (pontos[t] == melhor) {
                    grupo.add(t);
                }
            }
            restantes.removeAll(grupo);
            blocos.add(grupo);
            definitivo.add(grupo.size() == 1);
        }

        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (int i = 0; i < blocos.size(); i++) {
                if (definitivo.get(i)) {
                    continue;
                }
                List<Integer> bloco = blocos.get(i);
                boolean partiu = false;
                for (int c = 0; c < ordemCriterios.length && !partiu; c++) {
                    int[] chave = new int[nTimes];
                    for (int t : bloco) {
                        chave[t] = chaveDoCriterio(ordemCriterios[c], t, bloco, jogos, melhorDe,
                                pontosVitoria, pontosDerrotaDecisiva, pontosDerrota, favor, contra);
                    }
                    int primeira = chave[bloco.get(0)];
                    boolean todasIguais = true;
                    for (int t : bloco) {
                        if (chave[t] != primeira) {
                            todasIguais = false;
                        }
                    }
                    if (todasIguais) {
                        continue;
                    }
                    // Parte o bloco em sub-blocos, do maior valor de chave para o menor.
                    List<List<Integer>> partes = new ArrayList<>();
                    List<Integer> aDistribuir = new ArrayList<>(bloco);
                    while (!aDistribuir.isEmpty()) {
                        int maior = Integer.MIN_VALUE;
                        for (int t : aDistribuir) {
                            if (chave[t] > maior) {
                                maior = chave[t];
                            }
                        }
                        List<Integer> parte = new ArrayList<>();
                        for (int t : aDistribuir) {
                            if (chave[t] == maior) {
                                parte.add(t);
                            }
                        }
                        aDistribuir.removeAll(parte);
                        partes.add(parte);
                    }
                    blocos.remove(i);
                    definitivo.remove(i);
                    for (int k = 0; k < partes.size(); k++) {
                        blocos.add(i + k, partes.get(k));
                        definitivo.add(i + k, partes.get(k).size() == 1);
                    }
                    partiu = true;
                    mudou = true;
                }
                if (partiu) {
                    break;
                }
                // Nenhum critério separou: o bloco fica empatado de vez.
                definitivo.set(i, true);
                mudou = true;
            }
        }

        List<Posicao> tabela = new ArrayList<>();
        int posicaoAtual = 1;
        for (List<Integer> bloco : blocos) {
            List<Integer> ordenado = new ArrayList<>(bloco);
            java.util.Collections.sort(ordenado);
            for (int t : ordenado) {
                tabela.add(new Posicao(t, posicaoAtual, bloco.size() > 1, jogadas[t], vitorias[t], derrotas[t],
                        favor[t], contra[t], pontos[t]));
            }
            posicaoAtual += bloco.size();
        }
        return tabela;
    }

    private static int chaveDoCriterio(int criterio, int time, List<Integer> bloco, List<Jogo> jogos, int melhorDe,
                                       int pv, int pdd, int pd, int[] favor, int[] contra) {
        if (criterio == 1) {
            return favor[time] - contra[time];
        }
        if (criterio == 2) {
            return favor[time];
        }
        // Confronto direto: pontos só nas Partidas FINALIZADAS entre membros do bloco.
        int total = 0;
        for (Jogo j : jogos) {
            if (!j.finalizada() || !bloco.contains(j.a()) || !bloco.contains(j.b())) {
                continue;
            }
            if (j.a() != time && j.b() != time) {
                continue;
            }
            boolean venceu = (j.a() == time) == (j.jogosA() > j.jogosB());
            boolean decisiva = !j.walkover() && melhorDe > 1 && j.jogosA() + j.jogosB() == melhorDe;
            total += venceu ? pv : (decisiva ? pdd : pd);
        }
        return total;
    }
}
