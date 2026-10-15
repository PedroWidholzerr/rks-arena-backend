package com.rks.domain.standings;

import com.rks.domain.rules.EstadoPartida;
import com.rks.domain.rules.Pontuacao;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.rks.domain.standings.Dados.A;
import static com.rks.domain.standings.Dados.B;
import static com.rks.domain.standings.Dados.C;
import static com.rks.domain.standings.Dados.D;
import static com.rks.domain.standings.Dados.fin;
import static com.rks.domain.standings.Dados.linhaDe;
import static com.rks.domain.standings.Dados.ordem;
import static com.rks.domain.standings.Dados.wo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsTest {

    private static final RegrasClassificacao MD3 = RegrasClassificacao.padrao(Pontuacao.PADRAO, 3);
    private static final RegrasClassificacao MD1 = RegrasClassificacao.padrao(Pontuacao.PADRAO, 1);

    private static Classificacao calcular(List<UUID> times, List<PartidaResultado> partidas, RegrasClassificacao regras) {
        return Standings.compute(times, partidas, regras, List.of());
    }

    @Test
    void colunasDaTabelaVemDasPartidas() {
        // Melhor de 3, pontuação 2/1/0 (vitória/derrota decisiva/derrota).
        // A 2x0 B: A vence (2 pts); B perde sem decisiva (0 pt).
        // B 2x1 C: B vence (2 pts); C perde na decisiva (1 pt).
        Classificacao c = calcular(List.of(A, B, C), List.of(fin(A, B, 2, 0), fin(B, C, 2, 1)), MD3);

        Linha a = linhaDe(c, A);
        assertEquals(1, a.posicao());
        assertEquals(1, a.jogadas());
        assertEquals(1, a.vitorias());
        assertEquals(0, a.derrotas());
        assertEquals(2, a.jogosAFavor());
        assertEquals(0, a.jogosContra());
        assertEquals(2, a.saldo());
        assertEquals(2, a.pontos());

        Linha b = linhaDe(c, B);
        assertEquals(2, b.jogadas());
        assertEquals(1, b.vitorias());
        assertEquals(1, b.derrotas());
        assertEquals(2, b.jogosAFavor());
        assertEquals(3, b.jogosContra());
        assertEquals(-1, b.saldo());
        assertEquals(2, b.pontos());

        assertEquals(1, linhaDe(c, C).pontos());
    }

    @Test
    void soEntramPartidasFinalizadas() {
        List<PartidaResultado> partidas = List.of(
                fin(A, B, 2, 0),
                Dados.emEstado(EstadoPartida.AGENDADA, B, C),
                Dados.emEstado(EstadoPartida.EM_ANDAMENTO, B, C),
                Dados.emEstado(EstadoPartida.ANULADA, C, A));

        Classificacao c = calcular(List.of(A, B, C), partidas, MD3);

        assertEquals(1, linhaDe(c, A).jogadas());
        assertEquals(1, linhaDe(c, B).jogadas());
        assertEquals(0, linhaDe(c, C).jogadas());
    }

    @Test
    void walkoverVaiComoVitoriaEDerrotaNormais() {
        // Melhor de 3: W.O. 2x0 nunca é decisiva; o perdedor leva a pontuação de derrota (0).
        Classificacao c = calcular(List.of(A, B), List.of(wo(A, B, 2, 0)), MD3);

        assertEquals(2, linhaDe(c, A).pontos());
        assertEquals(0, linhaDe(c, B).pontos());
        assertEquals(1, linhaDe(c, A).vitorias());
        assertEquals(1, linhaDe(c, B).derrotas());
    }

    @Test
    void derrotaDecisivaValeAPontuacaoDeDerrotaDecisiva() {
        Classificacao c = calcular(List.of(A, B), List.of(fin(A, B, 2, 1)), MD3);

        assertEquals(2, linhaDe(c, A).pontos());
        assertEquals(1, linhaDe(c, B).pontos());
    }

    @Test
    void naMelhorDe1ADerrotaNuncaEhDecisiva() {
        // 1x0 na melhor de 1 não é decisiva: o perdedor leva a pontuação de derrota comum (0).
        Classificacao c = calcular(List.of(A, B), List.of(fin(A, B, 1, 0)), MD1);

        assertEquals(2, linhaDe(c, A).pontos());
        assertEquals(0, linhaDe(c, B).pontos());
    }

    @Test
    void pontuacaoPersonalizadaEhRespeitada() {
        RegrasClassificacao regras = RegrasClassificacao.padrao(new Pontuacao(3, 1, 0), 3);

        Classificacao c = calcular(List.of(A, B), List.of(fin(A, B, 2, 1)), regras);

        assertEquals(3, linhaDe(c, A).pontos());
        assertEquals(1, linhaDe(c, B).pontos());
    }

    @Test
    void posicoesSaemOrdenadasPorPontos() {
        // Pontuação 2/1/0, melhor de 3. A 2x0 B, A 2x0 C, B 2x0 C: A = 2+2 = 4, B = 0+2 = 2, C = 0, D = 0 (sem Partidas).
        // C e D empatam em 0 pontos. Sem confronto direto, o saldo de Jogos separa: C perdeu 0x2 duas vezes
        // (saldo -4) e D não jogou (saldo 0). Ordem: A, B, D, C, cada um na sua posição.
        Classificacao c = calcular(List.of(D, C, B, A),
                List.of(fin(A, B, 2, 0), fin(A, C, 2, 0), fin(B, C, 2, 0)), MD3);

        assertEquals(List.of(A, B, D, C), ordem(c));
        assertEquals(List.of(1, 2, 3, 4), c.linhas().stream().map(Linha::posicao).toList());
        assertTrue(c.linhas().stream().noneMatch(Linha::empate));
    }

    @Test
    void semPartidasTodosEmpatadosNaPrimeiraPosicao() {
        Classificacao c = calcular(List.of(A, B, C), List.of(), MD3);

        assertEquals(3, c.linhas().size());
        assertTrue(c.linhas().stream().allMatch(l -> l.posicao() == 1 && l.empate()));
    }

    @Test
    void semParticipantesDevolveTabelaVazia() {
        Classificacao c = calcular(List.of(), List.of(fin(A, B, 2, 0)), MD3);

        assertTrue(c.linhas().isEmpty());
        assertTrue(c.decisoesOrfas().isEmpty());
    }

    @Test
    void partidaComTimeForaDosParticipantesEhIgnorada() {
        Classificacao c = calcular(List.of(A, B), List.of(fin(A, C, 2, 0), fin(A, B, 2, 0)), MD3);

        assertEquals(1, linhaDe(c, A).jogadas());
        assertEquals(2, linhaDe(c, A).pontos());
    }

    @Test
    void ordemDasPartidasNaoMudaOResultado() {
        List<PartidaResultado> partidas = new ArrayList<>(List.of(
                fin(A, B, 2, 1), fin(B, C, 2, 0), fin(C, D, 2, 1), fin(D, A, 2, 0), fin(A, C, 2, 0), fin(B, D, 2, 1)));
        Classificacao base = calcular(List.of(A, B, C, D), partidas, MD3);

        Random aleatorio = new Random(7);
        for (int i = 0; i < 50; i++) {
            Collections.shuffle(partidas, aleatorio);
            List<UUID> times = new ArrayList<>(List.of(A, B, C, D));
            Collections.shuffle(times, aleatorio);
            assertEquals(base, calcular(times, partidas, MD3));
        }
    }

    @Test
    void calcularDuasVezesDaOMesmoResultado() {
        List<PartidaResultado> partidas = List.of(fin(A, B, 2, 1), fin(B, C, 2, 0));

        assertEquals(calcular(List.of(A, B, C), partidas, MD3), calcular(List.of(A, B, C), partidas, MD3));
    }

    @Test
    void editarEVoltarDevolveATabelaOriginal() {
        Classificacao original = calcular(List.of(A, B), List.of(fin(A, B, 2, 0)), MD3);
        Classificacao editada = calcular(List.of(A, B), List.of(fin(A, B, 0, 2)), MD3);
        Classificacao revertida = calcular(List.of(A, B), List.of(fin(A, B, 2, 0)), MD3);

        assertFalse(original.equals(editada));
        assertEquals(original, revertida);
    }

    @Test
    void anularEquivaleANuncaTerExistido() {
        Classificacao anulada = calcular(List.of(A, B, C),
                List.of(fin(A, B, 2, 0), Dados.emEstado(EstadoPartida.ANULADA, B, C)), MD3);
        Classificacao semAPartida = calcular(List.of(A, B, C), List.of(fin(A, B, 2, 0)), MD3);

        assertEquals(semAPartida, anulada);
    }
}
