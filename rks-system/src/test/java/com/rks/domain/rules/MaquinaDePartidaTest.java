package com.rks.domain.rules;

import com.rks.domain.rules.MaquinaDePartida.Placar;
import com.rks.domain.rules.MaquinaDePartida.Situacao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaquinaDePartidaTest {

    private static final Situacao AGENDADA = Situacao.agendada();
    private static final Situacao EM_ANDAMENTO = new Situacao(EstadoPartida.EM_ANDAMENTO, null);
    private static final Situacao FINALIZADA = new Situacao(EstadoPartida.FINALIZADA, new Placar(2, 1, false));

    @Test
    void iniciarVaiDeAgendadaParaEmAndamentoSemPlacar() {
        Situacao nova = MaquinaDePartida.iniciar(AGENDADA);

        assertEquals(EstadoPartida.EM_ANDAMENTO, nova.estado());
        assertNull(nova.placar());
    }

    @Test
    void iniciarSoValeAPartirDeAgendada() {
        assertThrows(TransicaoInvalidaException.class, () -> MaquinaDePartida.iniciar(EM_ANDAMENTO));
        assertThrows(TransicaoInvalidaException.class, () -> MaquinaDePartida.iniciar(FINALIZADA));
    }

    @Test
    void lancarResultadoFinalizaAPartidaAgendadaOuEmAndamento() {
        for (Situacao origem : new Situacao[]{AGENDADA, EM_ANDAMENTO}) {
            Situacao nova = MaquinaDePartida.lancarResultado(origem, 3, 2, 0);

            assertEquals(EstadoPartida.FINALIZADA, nova.estado());
            assertEquals(new Placar(2, 0, false), nova.placar());
        }
    }

    @Test
    void lancarResultadoComPlacarInvalidoFalha() {
        assertThrows(PlacarInvalidoException.class, () -> MaquinaDePartida.lancarResultado(AGENDADA, 3, 1, 1));
        assertThrows(PlacarInvalidoException.class, () -> MaquinaDePartida.lancarResultado(AGENDADA, 3, 3, 0));
        assertThrows(PlacarInvalidoException.class, () -> MaquinaDePartida.lancarResultado(AGENDADA, 1, 0, 0));
    }

    @Test
    void lancarResultadoSobreFinalizadaFalhaPoisEdicaoTemOutraAcao() {
        assertThrows(TransicaoInvalidaException.class, () -> MaquinaDePartida.lancarResultado(FINALIZADA, 3, 2, 0));
    }

    @Test
    void walkoverGravaMetadeArredondadaPraCimaPorZero() {
        assertEquals(new Placar(1, 0, true), MaquinaDePartida.registrarWalkover(AGENDADA, 1, true).placar());
        assertEquals(new Placar(0, 2, true), MaquinaDePartida.registrarWalkover(AGENDADA, 3, false).placar());
        assertEquals(new Placar(3, 0, true), MaquinaDePartida.registrarWalkover(EM_ANDAMENTO, 5, true).placar());
    }

    @Test
    void walkoverSoVaiDeAgendadaOuEmAndamento() {
        assertThrows(TransicaoInvalidaException.class, () -> MaquinaDePartida.registrarWalkover(FINALIZADA, 3, true));
    }

    @Test
    void editarResultadoSoEmFinalizadaEUsaAMesmaValidacao() {
        Situacao editada = MaquinaDePartida.editarResultado(FINALIZADA, 3, 1, 2);

        assertEquals(new Placar(1, 2, false), editada.placar());
        assertThrows(TransicaoInvalidaException.class, () -> MaquinaDePartida.editarResultado(AGENDADA, 3, 2, 0));
        assertThrows(PlacarInvalidoException.class, () -> MaquinaDePartida.editarResultado(FINALIZADA, 3, 2, 2));
    }

    @Test
    void editarUmWalkoverGeraResultadoNormal() {
        Situacao wo = MaquinaDePartida.registrarWalkover(AGENDADA, 3, true);

        assertTrue(wo.placar().walkover());
        assertEquals(false, MaquinaDePartida.editarResultado(wo, 3, 2, 1).placar().walkover());
    }

    @Test
    void anularVoltaParaAgendadaSemPlacar() {
        Situacao nova = MaquinaDePartida.anularResultado(FINALIZADA);

        assertEquals(EstadoPartida.AGENDADA, nova.estado());
        assertNull(nova.placar());
        assertThrows(TransicaoInvalidaException.class, () -> MaquinaDePartida.anularResultado(AGENDADA));
        assertThrows(TransicaoInvalidaException.class, () -> MaquinaDePartida.anularResultado(EM_ANDAMENTO));
    }

    @Test
    void placarSoExisteEmFinalizada() {
        assertThrows(IllegalArgumentException.class,
                () -> new Situacao(EstadoPartida.AGENDADA, new Placar(0, 0, false)));
        assertThrows(IllegalArgumentException.class, () -> new Situacao(EstadoPartida.FINALIZADA, null));
    }

    @Test
    void erroDeTransicaoTemCodigoEstavel() {
        TransicaoInvalidaException erro = assertThrows(TransicaoInvalidaException.class,
                () -> MaquinaDePartida.iniciar(FINALIZADA));

        assertEquals("TRANSICAO_INVALIDA", erro.codigo());
    }
}
