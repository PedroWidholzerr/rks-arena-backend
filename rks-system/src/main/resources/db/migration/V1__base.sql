-- V1: esquema inteiro da O1 (AD-19). Ajustes futuros entram como migração nova, nunca editando esta.
-- Nomes em português sem acento; UUID nas chaves, timestamptz nos instantes, smallint nos placares.

CREATE TABLE usuario (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         varchar(255) NOT NULL,
    senha_hash    varchar(100) NOT NULL,
    papel         varchar(20)  NOT NULL DEFAULT 'ADMIN',
    ativo         boolean      NOT NULL DEFAULT true,
    trocar_senha  boolean      NOT NULL DEFAULT true,
    criado_em     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_papel CHECK (papel = 'ADMIN')
);
CREATE UNIQUE INDEX ux_usuario_email ON usuario (lower(email));

CREATE TABLE temporada (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome         varchar(120) NOT NULL,
    data_inicio  date,
    data_fim     date,
    encerrada    boolean      NOT NULL DEFAULT false,
    criado_em    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_temporada_datas CHECK (data_fim IS NULL OR data_inicio IS NULL OR data_fim >= data_inicio)
);

CREATE TABLE campeonato (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    temporada_id             uuid         NOT NULL REFERENCES temporada (id),
    nome                     varchar(120) NOT NULL,
    descricao                text,
    modo                     varchar(20)  NOT NULL DEFAULT 'ARAM',
    tamanho_time             smallint     NOT NULL DEFAULT 5,
    melhor_de                smallint     NOT NULL DEFAULT 1,
    pontos_vitoria           smallint     NOT NULL DEFAULT 2,
    pontos_derrota_decisiva  smallint     NOT NULL DEFAULT 0,
    pontos_derrota           smallint     NOT NULL DEFAULT 1,
    limite_times             smallint,
    ordem_desempate          varchar(200) NOT NULL DEFAULT 'PONTOS,CONFRONTO_DIRETO,SALDO,VITORIAS',
    estado                   varchar(20)  NOT NULL DEFAULT 'EM_BREVE',
    criado_em                timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_campeonato_modo CHECK (modo IN ('ARAM', 'SUMMONERS_RIFT')),
    CONSTRAINT ck_campeonato_estado CHECK (estado IN ('EM_BREVE', 'EM_ANDAMENTO', 'ENCERRADO')),
    CONSTRAINT ck_campeonato_tamanho CHECK (tamanho_time >= 1),
    CONSTRAINT ck_campeonato_melhor_de CHECK (melhor_de >= 1 AND melhor_de % 2 = 1),
    CONSTRAINT ck_campeonato_limite CHECK (limite_times IS NULL OR limite_times >= 2),
    CONSTRAINT ck_campeonato_pontuacao CHECK (
        pontos_derrota >= 0 AND pontos_derrota_decisiva >= pontos_derrota
        AND pontos_vitoria > pontos_derrota_decisiva
    )
);
CREATE INDEX ix_campeonato_temporada ON campeonato (temporada_id);

-- Uma Fase implícita de pontos corridos por Campeonato na O1.
CREATE TABLE fase (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    campeonato_id  uuid         NOT NULL REFERENCES campeonato (id),
    nome           varchar(120) NOT NULL,
    formato        varchar(20)  NOT NULL DEFAULT 'PONTOS_CORRIDOS',
    ordem          smallint     NOT NULL DEFAULT 1,
    turnos         smallint     NOT NULL DEFAULT 1,
    params         jsonb        NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_fase_formato CHECK (formato = 'PONTOS_CORRIDOS'),
    CONSTRAINT ck_fase_turnos CHECK (turnos >= 1),
    CONSTRAINT ux_fase_ordem UNIQUE (campeonato_id, ordem)
);

CREATE TABLE "time" (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome       varchar(120) NOT NULL,
    tag        varchar(10)  NOT NULL,
    tag_key    varchar(10)  NOT NULL,
    logo_url   varchar(500),
    criado_em  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ux_time_tag_key UNIQUE (tag_key),
    CONSTRAINT ck_time_tag_key CHECK (tag_key = lower(tag_key))
);

CREATE TABLE jogador (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nick          varchar(60) NOT NULL,
    tag           varchar(20) NOT NULL,
    riot_id_key   varchar(90) NOT NULL,
    foto_url      varchar(500),
    criado_em     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_jogador_riot_id_key UNIQUE (riot_id_key),
    CONSTRAINT ck_jogador_riot_id_key CHECK (riot_id_key = lower(riot_id_key))
);

CREATE TABLE elenco (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    time_id       uuid NOT NULL REFERENCES "time" (id),
    temporada_id  uuid NOT NULL REFERENCES temporada (id),
    CONSTRAINT ux_elenco_time_temporada UNIQUE (time_id, temporada_id)
);

CREATE TABLE elenco_membro (
    elenco_id   uuid        NOT NULL REFERENCES elenco (id) ON DELETE CASCADE,
    jogador_id  uuid        NOT NULL REFERENCES jogador (id),
    funcao      varchar(20) NOT NULL DEFAULT 'TITULAR',
    capitao     boolean     NOT NULL DEFAULT false,
    PRIMARY KEY (elenco_id, jogador_id),
    CONSTRAINT ck_elenco_membro_funcao CHECK (funcao IN ('TITULAR', 'RESERVA'))
);
CREATE UNIQUE INDEX ux_elenco_membro_um_capitao ON elenco_membro (elenco_id) WHERE capitao;

-- Time no Campeonato (não confundir com a Inscrição da O3).
CREATE TABLE participacao_campeonato (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    campeonato_id  uuid NOT NULL REFERENCES campeonato (id),
    time_id        uuid NOT NULL REFERENCES "time" (id),
    elenco_id      uuid NOT NULL REFERENCES elenco (id),
    CONSTRAINT ux_participacao_campeonato_time UNIQUE (campeonato_id, time_id)
);

-- Garante um Jogador por Campeonato.
CREATE TABLE campeonato_jogador (
    campeonato_id  uuid NOT NULL REFERENCES campeonato (id),
    jogador_id     uuid NOT NULL REFERENCES jogador (id),
    PRIMARY KEY (campeonato_id, jogador_id)
);

CREATE TABLE partida (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    fase_id         uuid         NOT NULL REFERENCES fase (id),
    rodada          smallint     NOT NULL,
    posicao         smallint     NOT NULL,
    time_a_id       uuid REFERENCES "time" (id),
    time_b_id       uuid REFERENCES "time" (id),
    estado          varchar(20)  NOT NULL DEFAULT 'AGENDADA',
    placar_a        smallint,
    placar_b        smallint,
    walkover        boolean      NOT NULL DEFAULT false,
    agendada_para   timestamptz,
    stream_url      varchar(500),
    mvp_jogador_id  uuid REFERENCES jogador (id),
    version         bigint       NOT NULL DEFAULT 0,
    criado_em       timestamptz  NOT NULL DEFAULT now(),
    atualizado_em   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_partida_estado CHECK (estado IN ('AGENDADA', 'EM_ANDAMENTO', 'FINALIZADA', 'ANULADA')),
    -- AD-6: placar NULL fora de FINALIZADA, e presente (os dois) em FINALIZADA.
    CONSTRAINT ck_partida_placar_estado CHECK (
        (estado = 'FINALIZADA' AND placar_a IS NOT NULL AND placar_b IS NOT NULL)
        OR (estado <> 'FINALIZADA' AND placar_a IS NULL AND placar_b IS NULL)
    ),
    CONSTRAINT ck_partida_placar_nao_negativo CHECK (
        (placar_a IS NULL OR placar_a >= 0) AND (placar_b IS NULL OR placar_b >= 0)
    ),
    CONSTRAINT ck_partida_times_distintos CHECK (time_a_id IS NULL OR time_b_id IS NULL OR time_a_id <> time_b_id),
    CONSTRAINT ck_partida_walkover CHECK (NOT walkover OR estado = 'FINALIZADA'),
    CONSTRAINT ux_partida_posicao UNIQUE (fase_id, rodada, posicao)
);
CREATE INDEX ix_partida_fase_estado ON partida (fase_id, estado);
CREATE INDEX ix_partida_agendada_para ON partida (agendada_para);

CREATE TABLE decisao_desempate (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    fase_id    uuid         NOT NULL REFERENCES fase (id),
    times_ids  uuid[]       NOT NULL,
    ordem_ids  uuid[]       NOT NULL,
    motivo     text         NOT NULL,
    admin_id   uuid REFERENCES usuario (id),
    criado_em  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_decisao_desempate_tamanho CHECK (
        cardinality(times_ids) >= 2 AND cardinality(ordem_ids) = cardinality(times_ids)
    ),
    CONSTRAINT ck_decisao_desempate_motivo CHECK (length(btrim(motivo)) > 0)
);
CREATE INDEX ix_decisao_desempate_fase ON decisao_desempate (fase_id);

-- AD-23: guarda a resposta por 24 h para repetir a mesma resposta.
CREATE TABLE chave_idempotencia (
    chave   uuid        NOT NULL,
    escopo  varchar(80) NOT NULL,
    status  smallint    NOT NULL,
    corpo   jsonb,
    em      timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (chave, escopo)
);
CREATE INDEX ix_chave_idempotencia_em ON chave_idempotencia (em);

-- AD-22: gravado na mesma transação de toda mudança relevante.
CREATE TABLE audit_log (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    em            timestamptz NOT NULL DEFAULT now(),
    admin_id      uuid REFERENCES usuario (id),
    entidade      varchar(60) NOT NULL,
    entidade_id   uuid,
    acao          varchar(60) NOT NULL,
    antes         jsonb,
    depois        jsonb
);
CREATE INDEX ix_audit_log_entidade ON audit_log (entidade, entidade_id, em);
