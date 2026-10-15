package com.rks.infra.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "elenco_membro")
@IdClass(ElencoMembroId.class)
@Getter
@Setter
@NoArgsConstructor
public class ElencoMembroEntidade {

    @Id
    @Column(name = "elenco_id")
    private UUID elencoId;

    @Id
    @Column(name = "jogador_id")
    private UUID jogadorId;

    @Column(nullable = false)
    private String funcao = "TITULAR";

    @Column(nullable = false)
    private boolean capitao;

    public ElencoMembroEntidade(UUID elencoId, UUID jogadorId, String funcao, boolean capitao) {
        this.elencoId = elencoId;
        this.jogadorId = jogadorId;
        this.funcao = funcao;
        this.capitao = capitao;
    }
}
