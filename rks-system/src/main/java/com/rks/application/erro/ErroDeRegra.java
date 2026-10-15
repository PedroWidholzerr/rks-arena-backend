package com.rks.application.erro;

import java.util.Map;

/**
 * Erro de regra de negócio com código estável em maiúsculas (ex.: CONFIG_CONGELADA). A API o
 * traduz para application/problem+json: 404 não encontrado, 409 conflito de estado ou de versão,
 * 422 regra violada, 401 credencial inválida. Os {@code extras} viram propriedades do problema
 * (por exemplo, o número de Partidas finalizadas que congelam a configuração).
 */
public class ErroDeRegra extends RuntimeException {

    public enum Tipo { NAO_ENCONTRADO, CONFLITO, REGRA, NAO_AUTENTICADO }

    private final String codigo;
    private final Tipo tipo;
    private final Map<String, Object> extras;

    public ErroDeRegra(Tipo tipo, String codigo, String mensagem) {
        this(tipo, codigo, mensagem, Map.of());
    }

    public ErroDeRegra(Tipo tipo, String codigo, String mensagem, Map<String, Object> extras) {
        super(mensagem);
        this.tipo = tipo;
        this.codigo = codigo;
        this.extras = Map.copyOf(extras);
    }

    public static ErroDeRegra naoEncontrado(String codigo, String mensagem) {
        return new ErroDeRegra(Tipo.NAO_ENCONTRADO, codigo, mensagem);
    }

    public static ErroDeRegra conflito(String codigo, String mensagem) {
        return new ErroDeRegra(Tipo.CONFLITO, codigo, mensagem);
    }

    public static ErroDeRegra conflito(String codigo, String mensagem, Map<String, Object> extras) {
        return new ErroDeRegra(Tipo.CONFLITO, codigo, mensagem, extras);
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

    public Map<String, Object> extras() {
        return extras;
    }
}
