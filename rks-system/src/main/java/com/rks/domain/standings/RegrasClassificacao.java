package com.rks.domain.standings;

import com.rks.domain.rules.Pontuacao;

import java.util.EnumSet;
import java.util.List;

/** Regras vigentes do Campeonato para calcular a tabela. */
public record RegrasClassificacao(Pontuacao pontuacao, int melhorDe, List<CriterioDesempate> criterios) {

    public static final List<CriterioDesempate> CRITERIOS_PADRAO = List.of(
            CriterioDesempate.CONFRONTO_DIRETO, CriterioDesempate.SALDO, CriterioDesempate.JOGOS_VENCIDOS);

    public RegrasClassificacao {
        if (pontuacao == null || criterios == null) {
            throw new IllegalArgumentException("Pontuação e critérios são obrigatórios.");
        }
        if (melhorDe < 1 || melhorDe % 2 == 0) {
            throw new IllegalArgumentException("Melhor de N exige N ímpar maior ou igual a 1.");
        }
        if (criterios.size() != CriterioDesempate.values().length
                || !EnumSet.copyOf(criterios).equals(EnumSet.allOf(CriterioDesempate.class))) {
            throw new IllegalArgumentException("Os critérios devem listar cada critério de desempate exatamente uma vez.");
        }
        criterios = List.copyOf(criterios);
    }

    public static RegrasClassificacao padrao(Pontuacao pontuacao, int melhorDe) {
        return new RegrasClassificacao(pontuacao, melhorDe, CRITERIOS_PADRAO);
    }
}
