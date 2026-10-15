package com.rks.domain.rules;

/**
 * Máquina de estados da Partida (AD-6), como funções puras.
 * <pre>
 * AGENDADA -> EM_ANDAMENTO -> FINALIZADA
 * AGENDADA -> FINALIZADA (placar ou W.O.)
 * FINALIZADA -> AGENDADA (anular resultado, placar NULL)
 * AGENDADA -> ANULADA (cancelar) e ANULADA -> AGENDADA: só na O3, fora desta classe
 * </pre>
 * O placar é {@code null} fora de FINALIZADA. 0x0 nunca representa "não jogada".
 */
public final class MaquinaDePartida {

    private MaquinaDePartida() {
    }

    /** Resultado gravado numa Partida FINALIZADA. */
    public record Placar(int jogosA, int jogosB, boolean walkover) {
    }

    /** Estado da Partida e seu placar ({@code null} fora de FINALIZADA). */
    public record Situacao(EstadoPartida estado, Placar placar) {

        public Situacao {
            if ((estado == EstadoPartida.FINALIZADA) != (placar != null)) {
                throw new IllegalArgumentException("O placar existe se e somente se a Partida está FINALIZADA.");
            }
        }

        public static Situacao agendada() {
            return new Situacao(EstadoPartida.AGENDADA, null);
        }
    }

    /** AGENDADA -> EM_ANDAMENTO, sem placar. */
    public static Situacao iniciar(Situacao atual) {
        exigirEstado(atual, "iniciar", EstadoPartida.AGENDADA);
        return new Situacao(EstadoPartida.EM_ANDAMENTO, null);
    }

    /** AGENDADA ou EM_ANDAMENTO -> FINALIZADA, com o placar validado pelo AD-3. */
    public static Situacao lancarResultado(Situacao atual, int melhorDe, int jogosA, int jogosB) {
        exigirEstado(atual, "lançar o resultado de", EstadoPartida.AGENDADA, EstadoPartida.EM_ANDAMENTO);
        RegrasDePlacar.validar(melhorDe, jogosA, jogosB);
        return new Situacao(EstadoPartida.FINALIZADA, new Placar(jogosA, jogosB, false));
    }

    /** AGENDADA ou EM_ANDAMENTO -> FINALIZADA por W.O.: ⌈N/2⌉x0 para o vencedor indicado. */
    public static Situacao registrarWalkover(Situacao atual, int melhorDe, boolean vencedorEhA) {
        exigirEstado(atual, "registrar W.O. em", EstadoPartida.AGENDADA, EstadoPartida.EM_ANDAMENTO);
        int necessarios = RegrasDePlacar.jogosParaVencer(melhorDe);
        int a = vencedorEhA ? necessarios : 0;
        int b = vencedorEhA ? 0 : necessarios;
        RegrasDePlacar.validarWalkover(melhorDe, a, b);
        return new Situacao(EstadoPartida.FINALIZADA, new Placar(a, b, true));
    }

    /** Edição de resultado: só em FINALIZADA, pela mesma validação do AD-3. Vira resultado normal, sem W.O. */
    public static Situacao editarResultado(Situacao atual, int melhorDe, int jogosA, int jogosB) {
        exigirEstado(atual, "editar o resultado de", EstadoPartida.FINALIZADA);
        RegrasDePlacar.validar(melhorDe, jogosA, jogosB);
        return new Situacao(EstadoPartida.FINALIZADA, new Placar(jogosA, jogosB, false));
    }

    /** Anular resultado: FINALIZADA -> AGENDADA com placar NULL. */
    public static Situacao anularResultado(Situacao atual) {
        exigirEstado(atual, "anular o resultado de", EstadoPartida.FINALIZADA);
        return Situacao.agendada();
    }

    private static void exigirEstado(Situacao atual, String acao, EstadoPartida... permitidos) {
        for (EstadoPartida estado : permitidos) {
            if (atual.estado() == estado) {
                return;
            }
        }
        throw new TransicaoInvalidaException("Não é possível " + acao + " uma Partida " + atual.estado() + ".");
    }
}
