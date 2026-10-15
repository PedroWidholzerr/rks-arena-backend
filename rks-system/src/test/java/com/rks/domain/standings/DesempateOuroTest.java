package com.rks.domain.standings;

import com.rks.domain.rules.Pontuacao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.rks.domain.standings.CriterioDesempate.CONFRONTO_DIRETO;
import static com.rks.domain.standings.CriterioDesempate.JOGOS_VENCIDOS;
import static com.rks.domain.standings.CriterioDesempate.SALDO;
import static com.rks.domain.standings.Dados.A;
import static com.rks.domain.standings.Dados.B;
import static com.rks.domain.standings.Dados.C;
import static com.rks.domain.standings.Dados.D;
import static com.rks.domain.standings.Dados.E;
import static com.rks.domain.standings.Dados.fin;
import static com.rks.domain.standings.Dados.linhaDe;
import static com.rks.domain.standings.Dados.ordem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tabelas de ouro do desempate (AD-4, AD-5), calculadas à mão. Todas na melhor de 3.
 * Jogos: "2x0" quer dizer que o primeiro Time venceu por 2 a 0.
 */
class DesempateOuroTest {

    private static final RegrasClassificacao PADRAO_MD3 = RegrasClassificacao.padrao(Pontuacao.PADRAO, 3);
    /** Pontuação 3/0/0: pontos = 3 x vitórias, o que deixa o empate de pontos fácil de montar à mão. */
    private static final RegrasClassificacao TRES_PONTOS = RegrasClassificacao.padrao(new Pontuacao(3, 0, 0), 3);

    private static Classificacao calcular(List<UUID> times, List<PartidaResultado> partidas, RegrasClassificacao regras) {
        return Standings.compute(times, partidas, regras, List.of());
    }

    @Test
    void doisEmpatadosOConfrontoDiretoDecide() {
        // Pontuação 2/1/0 (vitória, derrota decisiva, derrota). Partidas:
        //   A 2x0 B -> A +2, B +0 (2x0 não é decisiva)
        //   B 2x1 C -> B +2, C +1 (2x1 é decisiva)
        //   C 2x0 A -> C +2, A +0
        // Pontos: A = 2+0 = 2, B = 0+2 = 2, C = 1+2 = 3, D = 0.
        // C lidera com 3. A e B empatam em 2. Confronto direto, só A x B: A venceu -> A 2 pts, B 0 pt. A fica na frente.
        Classificacao c = calcular(List.of(A, B, C, D),
                List.of(fin(A, B, 2, 0), fin(B, C, 2, 1), fin(C, A, 2, 0)), PADRAO_MD3);

        assertEquals(List.of(C, A, B, D), ordem(c));
        assertEquals(List.of(1, 2, 3, 4), c.linhas().stream().map(Linha::posicao).toList());
        assertTrue(c.linhas().stream().noneMatch(Linha::empate));
    }

    @Test
    void doisEmpatadosSemConfrontoDecidemPeloSaldo() {
        // Partidas: A 2x0 C -> A +2, C +0. B 2x1 C -> B +2, C +1.
        // Pontos: A = 2, B = 2, C = 0+1 = 1, D = 0.
        // A e B não se enfrentaram: a mini-tabela dá 0 a 0, o confronto direto não separa.
        // Saldo de Jogos: A = 2-0 = +2; B = 2-1 = +1. A fica na frente.
        Classificacao c = calcular(List.of(A, B, C, D),
                List.of(fin(A, C, 2, 0), fin(B, C, 2, 1)), PADRAO_MD3);

        assertEquals(List.of(A, B, C, D), ordem(c));
        assertTrue(c.linhas().stream().noneMatch(Linha::empate));
    }

    @Test
    void tresEmpatadosComCriterioParcialOsRestantesRecomecam() {
        // Pontuação 3/0/0. Partidas (todas 2x0, exceto C x D e C x E que são 2x1):
        //   A 2x0 B, A 2x0 C, D 2x0 A, E 2x0 A, B 2x0 D, B 2x0 E, C 2x1 D, C 2x1 E.
        // Vitórias: A = 2 (B, C); B = 2 (D, E); C = 2 (D, E); D = 1 (A); E = 1 (A).
        // Pontos: A = B = C = 6; D = E = 3.
        // Empate A, B, C. Mini-tabela só entre eles: A venceu B e C = 6; B = 0; C = 0.
        // O confronto direto separa só A (primeiro). B e C ficam e recomeçam pelo confronto direto:
        // B x C não se enfrentaram, 0 a 0, não separa. Saldo de Jogos, em todas as Partidas:
        //   B: ganhou 0 (A) + 2 (D) + 2 (E) = 4, perdeu 2 (A) = saldo +2.
        //   C: ganhou 0 (A) + 2 (D) + 2 (E) = 4, perdeu 2 (A) + 1 (D) + 1 (E) = 4, saldo 0.
        // B fica na frente de C.
        // Empate D, E (3 pts): confronto direto não há; saldo: D ganhou 2 (A) + 0 (B) + 1 (C) = 3,
        // perdeu 0 + 2 + 2 = 4, saldo -1; E igual, saldo -1; Jogos vencidos 3 e 3. Nada separa: empate.
        List<PartidaResultado> partidas = List.of(
                fin(A, B, 2, 0), fin(A, C, 2, 0), fin(D, A, 2, 0), fin(E, A, 2, 0),
                fin(B, D, 2, 0), fin(B, E, 2, 0), fin(C, D, 2, 1), fin(C, E, 2, 1));

        Classificacao c = calcular(List.of(A, B, C, D, E), partidas, TRES_PONTOS);

        assertEquals(List.of(A, B, C), ordem(c).subList(0, 3));
        assertEquals(List.of(1, 2, 3, 4, 4), c.linhas().stream().map(Linha::posicao).toList());
        assertEquals(Set.of(D, E), Set.copyOf(ordem(c).subList(3, 5)));
        assertTrue(linhaDe(c, D).empate());
        assertTrue(linhaDe(c, E).empate());
        assertFalse(linhaDe(c, A).empate());
        assertFalse(linhaDe(c, B).empate());
        assertFalse(linhaDe(c, C).empate());
        assertEquals(6, linhaDe(c, A).pontos());
        assertEquals(3, linhaDe(c, D).pontos());
        assertEquals(2, linhaDe(c, B).saldo());
        assertEquals(0, linhaDe(c, C).saldo());
        assertEquals(-1, linhaDe(c, D).saldo());
    }

    @Test
    void quatroEmpatadosSeParteEmDoisESubgruposRecomecam() {
        // Pontuação 3/0/0. Partidas (todas 2x0):
        //   A 2x0 B, A 2x0 C, B 2x0 C, B 2x0 D, C 2x0 D, D 2x0 A, C 2x0 E, D 2x0 E.
        // Vitórias: A = 2 (B, C); B = 2 (C, D); C = 2 (D, E); D = 2 (A, E). Pontos: A = B = C = D = 6; E = 0.
        // Mini-tabela entre A, B, C e D (E fora): A venceu B e C = 6; B venceu C e D = 6;
        // C venceu D = 3; D venceu A = 3. O confronto direto parte em {A, B} (6) e {C, D} (3).
        // {A, B} recomeçam: A venceu B, então A fica na frente. {C, D}: C venceu D, então C na frente.
        // E, com 0 pontos, fica em último.
        List<PartidaResultado> partidas = List.of(
                fin(A, B, 2, 0), fin(A, C, 2, 0), fin(B, C, 2, 0), fin(B, D, 2, 0),
                fin(C, D, 2, 0), fin(D, A, 2, 0), fin(C, E, 2, 0), fin(D, E, 2, 0));

        Classificacao c = calcular(List.of(A, B, C, D, E), partidas, TRES_PONTOS);

        assertEquals(List.of(A, B, C, D, E), ordem(c));
        assertEquals(List.of(1, 2, 3, 4, 5), c.linhas().stream().map(Linha::posicao).toList());
        assertTrue(c.linhas().stream().noneMatch(Linha::empate));
    }

    @Test
    void reordenarOsCriteriosMudaOResultado() {
        // Pontuação 3/0/0. Partidas: A 2x1 B, A 2x1 D, B 2x0 C, B 2x0 D.
        // Vitórias: A = 2 (B, D); B = 2 (C, D); C = 0; D = 0. Pontos: A = B = 6; C = D = 0.
        // Saldo de Jogos: A: ganhou 2+2 = 4, perdeu 1+1 = 2, saldo +2.
        //                 B: ganhou 1 (contra A) + 2 + 2 = 5, perdeu 2 (A) = saldo +3.
        // Ordem padrão (confronto direto primeiro): A venceu B, então A fica na frente.
        // Ordem [saldo, confronto direto, Jogos vencidos]: B tem saldo +3 contra +2, então B fica na frente.
        // C x D (0 pts): C perdeu 0x2 para B, saldo -2; D perdeu 1x2 para A e 0x2 para B, saldo 1-4 = -3. C na frente em ambas.
        List<PartidaResultado> partidas = List.of(
                fin(A, B, 2, 1), fin(A, D, 2, 1), fin(B, C, 2, 0), fin(B, D, 2, 0));

        Classificacao padrao = calcular(List.of(A, B, C, D), partidas, TRES_PONTOS);
        Classificacao saldoPrimeiro = calcular(List.of(A, B, C, D), partidas,
                new RegrasClassificacao(new Pontuacao(3, 0, 0), 3, List.of(SALDO, CONFRONTO_DIRETO, JOGOS_VENCIDOS)));

        assertEquals(List.of(A, B, C, D), ordem(padrao));
        assertEquals(List.of(B, A, C, D), ordem(saldoPrimeiro));
    }

    @Test
    void jogosVencidosSeparaQuandoOSaldoEmpata() {
        // Pontuação 3/0/0, ordem [saldo, Jogos vencidos, confronto direto]. Partidas:
        //   A 2x0 C, D 2x0 A, B 2x1 D, C 2x1 B.
        // Vitórias: A = 1 (C), D = 1 (A), B = 1 (D), C = 1 (B). Pontos: todos 3, empate de 4.
        // Saldo de Jogos: A = +2 (C) -2 (D) = 0; B = +1 (D) -1 (C) = 0; C = -2 (A) +1 (B) = -1; D = +2 (A) -1 (B) = +1.
        // O saldo parte em D (+1), {A, B} (0) e C (-1). Em {A, B} o saldo empata, então decide o critério seguinte:
        // Jogos vencidos: A = 2 + 0 = 2; B = 2 + 1 = 3. B fica na frente de A. Ordem final: D, B, A, C.
        Classificacao c = calcular(List.of(A, B, C, D),
                List.of(fin(A, C, 2, 0), fin(D, A, 2, 0), fin(B, D, 2, 1), fin(C, B, 2, 1)),
                new RegrasClassificacao(new Pontuacao(3, 0, 0), 3, List.of(SALDO, JOGOS_VENCIDOS, CONFRONTO_DIRETO)));

        assertEquals(List.of(D, B, A, C), ordem(c));
        assertTrue(c.linhas().stream().noneMatch(Linha::empate));
    }

    @Test
    void decisaoManualResolveOEmpateDoConjuntoExato() {
        // Mesmo cenário do teste de 3 empatados: D e E ficam empatados na posição 4.
        // O administrador decide E antes de D: E na posição 4, D na 5, sem marcar empate.
        List<PartidaResultado> partidas = List.of(
                fin(A, B, 2, 0), fin(A, C, 2, 0), fin(D, A, 2, 0), fin(E, A, 2, 0),
                fin(B, D, 2, 0), fin(B, E, 2, 0), fin(C, D, 2, 1), fin(C, E, 2, 1));
        DecisaoManual decisao = new DecisaoManual(List.of(E, D), "sorteio presencial");

        Classificacao c = Standings.compute(List.of(A, B, C, D, E), partidas, TRES_PONTOS, List.of(decisao));

        assertEquals(List.of(A, B, C, E, D), ordem(c));
        assertEquals(List.of(1, 2, 3, 4, 5), c.linhas().stream().map(Linha::posicao).toList());
        assertTrue(c.linhas().stream().noneMatch(Linha::empate));
        assertTrue(c.decisoesOrfas().isEmpty());
    }

    @Test
    void decisaoManualDeOutroConjuntoFicaOrfaESemEfeito() {
        // A decisão é para {A, B}, mas o empate atual é {D, E}: não se aplica, vira órfã e a tabela não muda.
        List<PartidaResultado> partidas = List.of(
                fin(A, B, 2, 0), fin(A, C, 2, 0), fin(D, A, 2, 0), fin(E, A, 2, 0),
                fin(B, D, 2, 0), fin(B, E, 2, 0), fin(C, D, 2, 1), fin(C, E, 2, 1));
        DecisaoManual orfa = new DecisaoManual(List.of(B, A), "decisão antiga");

        Classificacao semDecisao = calcular(List.of(A, B, C, D, E), partidas, TRES_PONTOS);
        Classificacao comDecisao = Standings.compute(List.of(A, B, C, D, E), partidas, TRES_PONTOS, List.of(orfa));

        assertEquals(semDecisao.linhas(), comDecisao.linhas());
        assertEquals(List.of(orfa), comDecisao.decisoesOrfas());
    }

    @Test
    void decisaoManualQueCobreSoPartedoEmpateNaoSeAplica() {
        // Todos sem partidas: empate de {A, B, C}. A decisão cobre só {A, B}: o conjunto não é igual, fica órfã.
        DecisaoManual parcial = new DecisaoManual(List.of(B, A), "parcial");

        Classificacao c = Standings.compute(List.of(A, B, C), List.of(), TRES_PONTOS, List.of(parcial));

        assertTrue(c.linhas().stream().allMatch(l -> l.posicao() == 1 && l.empate()));
        assertEquals(List.of(parcial), c.decisoesOrfas());
    }

    @Test
    void empateSemDecisaoSaiEmOrdemEstavelPorIdentificador() {
        // Sem partidas e sem decisão, a ordem dentro do empate não depende da ordem de entrada.
        Classificacao c1 = calcular(List.of(C, A, B), List.of(), TRES_PONTOS);
        Classificacao c2 = calcular(List.of(B, C, A), List.of(), TRES_PONTOS);

        assertEquals(c1, c2);
        assertEquals(List.of(A, B, C), ordem(c1));
    }
}
