package com.rks.domain.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegrasDePlacarTest {

    @Test
    void jogosParaVencerEhOTetoDaMetade() {
        assertEquals(1, RegrasDePlacar.jogosParaVencer(1));
        assertEquals(2, RegrasDePlacar.jogosParaVencer(3));
        assertEquals(3, RegrasDePlacar.jogosParaVencer(5));
    }

    @Test
    void melhorDeParOuMenorQueUmEhRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> RegrasDePlacar.jogosParaVencer(2));
        assertThrows(IllegalArgumentException.class, () -> RegrasDePlacar.jogosParaVencer(0));
        assertThrows(IllegalArgumentException.class, () -> RegrasDePlacar.jogosParaVencer(-3));
    }

    @Test
    void melhorDe1AceitaApenas1x0E0x1() {
        RegrasDePlacar.validar(1, 1, 0);
        RegrasDePlacar.validar(1, 0, 1);
        invalido(1, 0, 0);
        invalido(1, 1, 1);
        invalido(1, 2, 0);
        invalido(1, -1, 0);
    }

    @Test
    void melhorDe3AceitaApenas2x0E2x1() {
        RegrasDePlacar.validar(3, 2, 0);
        RegrasDePlacar.validar(3, 2, 1);
        RegrasDePlacar.validar(3, 0, 2);
        RegrasDePlacar.validar(3, 1, 2);
        invalido(3, 3, 0);
        invalido(3, 1, 1);
        invalido(3, 2, 2);
        invalido(3, 1, 0);
        invalido(3, 0, 0);
    }

    @Test
    void melhorDe5AceitaApenas3x0A3x2() {
        for (int perdedor = 0; perdedor <= 2; perdedor++) {
            RegrasDePlacar.validar(5, 3, perdedor);
            RegrasDePlacar.validar(5, perdedor, 3);
        }
        invalido(5, 2, 2);
        invalido(5, 3, 3);
        invalido(5, 4, 0);
        invalido(5, 2, 1);
    }

    @Test
    void erroDePlacarTemCodigoEstavel() {
        PlacarInvalidoException erro = assertThrows(PlacarInvalidoException.class, () -> RegrasDePlacar.validar(3, 1, 1));
        assertEquals("PLACAR_INVALIDO", erro.codigo());
    }

    @Test
    void walkoverVale2x0Na3E1x0Na1() {
        RegrasDePlacar.validarWalkover(1, 1, 0);
        RegrasDePlacar.validarWalkover(3, 0, 2);
        RegrasDePlacar.validarWalkover(5, 3, 0);
        assertThrows(PlacarInvalidoException.class, () -> RegrasDePlacar.validarWalkover(3, 2, 1));
        assertThrows(PlacarInvalidoException.class, () -> RegrasDePlacar.validarWalkover(5, 2, 0));
        assertThrows(PlacarInvalidoException.class, () -> RegrasDePlacar.validarWalkover(3, 0, 0));
    }

    @Test
    void decisivaNaMelhorDe1NuncaOcorre() {
        assertFalse(RegrasDePlacar.ehDecisiva(1, 1, 0, false));
        assertFalse(RegrasDePlacar.ehDecisiva(1, 0, 1, false));
    }

    @Test
    void decisivaNaMelhorDe3EhSo2x1() {
        assertTrue(RegrasDePlacar.ehDecisiva(3, 2, 1, false));
        assertTrue(RegrasDePlacar.ehDecisiva(3, 1, 2, false));
        assertFalse(RegrasDePlacar.ehDecisiva(3, 2, 0, false));
        assertFalse(RegrasDePlacar.ehDecisiva(3, 0, 2, false));
    }

    @Test
    void decisivaNaMelhorDe5EhSo3x2() {
        assertTrue(RegrasDePlacar.ehDecisiva(5, 3, 2, false));
        assertFalse(RegrasDePlacar.ehDecisiva(5, 3, 1, false));
        assertFalse(RegrasDePlacar.ehDecisiva(5, 3, 0, false));
    }

    @Test
    void walkoverNuncaEhDecisiva() {
        assertFalse(RegrasDePlacar.ehDecisiva(3, 2, 0, true));
        assertFalse(RegrasDePlacar.ehDecisiva(5, 3, 0, true));
        assertFalse(RegrasDePlacar.ehDecisiva(1, 1, 0, true));
    }

    @Test
    void pontuacaoPadraoEhVitoria2DerrotaDecisiva1Derrota0() {
        assertEquals(new Pontuacao(2, 1, 0), Pontuacao.PADRAO);
    }

    @Test
    void pontuacaoRejeitaNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new Pontuacao(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Pontuacao(3, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Pontuacao(3, 1, -1));
    }

    @Test
    void pontuacaoExigeVitoriaMaiorQueDerrotaDecisivaMaiorOuIgualADerrota() {
        new Pontuacao(3, 0, 0);
        new Pontuacao(3, 1, 1);
        new Pontuacao(3, 1, 0);
        assertThrows(IllegalArgumentException.class, () -> new Pontuacao(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Pontuacao(2, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Pontuacao(0, 0, 0));
    }

    private static void invalido(int melhorDe, int a, int b) {
        assertThrows(PlacarInvalidoException.class, () -> RegrasDePlacar.validar(melhorDe, a, b),
                "melhor de " + melhorDe + " com " + a + "x" + b);
    }
}
