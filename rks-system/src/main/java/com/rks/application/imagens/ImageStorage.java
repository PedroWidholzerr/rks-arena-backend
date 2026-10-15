package com.rks.application.imagens;

/**
 * Porta de armazenamento de imagens (AD-16). A implementação guarda fora do servidor (Supabase
 * Storage), então nada depende de disco local, que não sobrevive a redeploy.
 */
public interface ImageStorage {

    /** Envia o objeto. Falha com {@code ErroDeRegra} 503 se o armazenamento não responder. */
    void put(String chave, byte[] conteudo, String contentType);

    /** Apaga o objeto; chave inexistente não é erro. */
    void delete(String chave);

    /** URL pública da chave, ou {@code null} para chave nula. */
    String publicUrl(String chave);
}
