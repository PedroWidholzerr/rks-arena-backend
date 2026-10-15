package com.rks.application.erro;

/**
 * Erro de regra de negócio com código estável em maiúsculas (ex.: CONFIG_CONGELADA). A API o
 * traduz para application/problem+json: 404 não encontrado, 409 conflito de estado ou de versão,
 * 422 regra violada, 401 credencial inválida.
 */
public class ErroDeRegra extends RuntimeException {

    public enum Tipo { NAO_ENCONTRADO, CONFLITO, REGRA, NAO_AUTENTICADO }

    private final String codigo;
    private final Tipo tipo;

    public ErroDeRegra(Tipo tipo, String codigo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
        this.codigo = codigo;
    }

    public static ErroDeRegra naoEncontrado(String codigo, String mensagem) {
        return new ErroDeRegra(Tipo.NAO_ENCONTRADO, codigo, mensagem);
    }

    public static ErroDeRegra conflito(String codigo, String mensagem) {
        return new ErroDeRegra(Tipo.CONFLITO, codigo, mensagem);
    }

    public static ErroDeRegra regra(String codigo, String mensagem) {
        return new ErroDeRegra(Tipo.REGRA, codigo, mensagem);
    }

    public String codigo() {
        return codigo;
    }

    public Tipo tipo() {
        return tipo;
    }
}
