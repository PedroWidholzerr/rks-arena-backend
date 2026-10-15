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

@Entity
@Table(name = "jogador")
@Getter
@Setter
@NoArgsConstructor
public class JogadorEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nick;

    @Column(nullable = false)
    private String tag;

    @Column(name = "riot_id_key", nullable = false)
    private String riotIdKey;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();
}
