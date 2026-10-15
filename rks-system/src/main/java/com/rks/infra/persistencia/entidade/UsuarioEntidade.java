package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Administrador. Só existe o papel ADMIN; não há contas de time nem de jogador. */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntidade {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private String papel = "ADMIN";

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "trocar_senha", nullable = false)
    private boolean trocarSenha = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    public UsuarioEntidade(String email, String senhaHash, boolean trocarSenha) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.senhaHash = senhaHash;
        this.trocarSenha = trocarSenha;
    }
}
