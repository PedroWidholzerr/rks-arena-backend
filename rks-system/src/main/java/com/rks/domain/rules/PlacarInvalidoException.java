package com.rks.domain.rules;

/** Placar que a regra do AD-3 não aceita. O código estável é usado pela API (422). */
public class PlacarInvalidoException extends RuntimeException {

    public static final String CODIGO = "PLACAR_INVALIDO";

    public PlacarInvalidoException(String mensagem) {
        super(mensagem);
    }

    public String codigo() {
        return CODIGO;
    }
}
