package com.rks.domain.rules;

/**
 * Regras de placar do AD-3, num lugar só: validação, W.O. e Partida decisiva.
 * "Jogo" é cada mapa disputado; o placar é o número de Jogos vencidos por cada Time.
 */
public final class RegrasDePlacar {

    private RegrasDePlacar() {
    }

    /** Jogos que o vencedor precisa ganhar: ⌈N/2⌉ (melhor de 1 = 1, de 3 = 2, de 5 = 3). */
    public static int jogosParaVencer(int melhorDe) {
        exigirMelhorDeValido(melhorDe);
        return (melhorDe + 1) / 2;
    }

    /**
     * Valida um placar lançado: vencedor com exatamente ⌈N/2⌉ Jogos, perdedor com menos,
     * sem empate. Lança {@link PlacarInvalidoException} caso contrário.
     */
    public static void validar(int melhorDe, int jogosA, int jogosB) {
        int necessarios = jogosParaVencer(melhorDe);
        if (jogosA < 0 || jogosB < 0) {
            throw new PlacarInvalidoException("O placar não aceita valores negativos.");
        }
        boolean aVenceu = jogosA == necessarios && jogosB < necessarios;
        boolean bVenceu = jogosB == necessarios && jogosA < necessarios;
        if (!aVenceu && !bVenceu) {
            throw new PlacarInvalidoException("Na melhor de " + melhorDe + " o vencedor tem exatamente "
                    + necessarios + " Jogos e o perdedor menos que isso; recebido " + jogosA + "x" + jogosB + ".");
        }
    }

    /** W.O. grava ⌈N/2⌉×0 para quem vence. Lança {@link PlacarInvalidoException} se não for isso. */
    public static void validarWalkover(int melhorDe, int jogosA, int jogosB) {
        int necessarios = jogosParaVencer(melhorDe);
        boolean aVenceu = jogosA == necessarios && jogosB == 0;
        boolean bVenceu = jogosB == necessarios && jogosA == 0;
        if (!aVenceu && !bVenceu) {
            throw new PlacarInvalidoException("O W.O. na melhor de " + melhorDe + " vale " + necessarios
                    + "x0; recebido " + jogosA + "x" + jogosB + ".");
        }
    }

    /**
     * Partida decisiva: foi até o último Jogo possível. Na melhor de 1, 1x0 não é decisiva,
     * e W.O. nunca é.
     */
    public static boolean ehDecisiva(int melhorDe, int jogosA, int jogosB, boolean walkover) {
        return !walkover && melhorDe > 1 && jogosA + jogosB == melhorDe;
    }

    private static void exigirMelhorDeValido(int melhorDe) {
        if (melhorDe < 1 || melhorDe % 2 == 0) {
            throw new IllegalArgumentException("Melhor de N exige N ímpar maior ou igual a 1; recebido " + melhorDe + ".");
        }
    }
}
