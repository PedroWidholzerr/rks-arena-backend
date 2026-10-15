package com.rks.domain.rules;

/** Transição de estado que a máquina da Partida (AD-6) não permite. A API responde 409. */
public class TransicaoInvalidaException extends RuntimeException {

    public static final String CODIGO = "TRANSICAO_INVALIDA";

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    public String codigo() {
        return CODIGO;
    }
}
