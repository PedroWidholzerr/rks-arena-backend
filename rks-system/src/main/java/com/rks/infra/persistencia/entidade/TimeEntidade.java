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
@Table(name = "\"time\"")
@Getter
@Setter
@NoArgsConstructor
public class TimeEntidade {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String tag;

    @Column(name = "tag_key", nullable = false)
    private String tagKey;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();
}
