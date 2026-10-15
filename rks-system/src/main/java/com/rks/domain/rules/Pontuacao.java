package com.rks.domain.rules;

/**
 * Pontuação do Campeonato (FR-11). Valores inteiros não negativos, com
 * vitória &gt; derrota decisiva &ge; derrota. O padrão "2/0/1" do PRD é vitória 2, derrota 0 e
 * derrota decisiva 1; na ordem deste record, {@code (2, 1, 0)}.
 */
public record Pontuacao(int vitoria, int derrotaDecisiva, int derrota) {

    public static final Pontuacao PADRAO = new Pontuacao(2, 1, 0);

    public Pontuacao {
        if (vitoria < 0 || derrotaDecisiva < 0 || derrota < 0) {
            throw new IllegalArgumentException("A pontuação não aceita valores negativos.");
        }
        if (!(vitoria > derrotaDecisiva && derrotaDecisiva >= derrota)) {
            throw new IllegalArgumentException(
                    "A pontuação exige vitória > derrota decisiva >= derrota.");
        }
    }
}
