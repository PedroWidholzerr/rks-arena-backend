package com.rks.domain.standings;

import java.util.List;

/**
 * Resultado de {@link Standings#compute}: a tabela e as decisões manuais que não se
 * aplicaram a nenhum empate atual (órfãs). Órfãs nunca são apagadas por efeito colateral.
 */
public record Classificacao(List<Linha> linhas, List<DecisaoManual> decisoesOrfas) {

    public Classificacao {
        linhas = List.copyOf(linhas);
        decisoesOrfas = List.copyOf(decisoesOrfas);
    }
}
