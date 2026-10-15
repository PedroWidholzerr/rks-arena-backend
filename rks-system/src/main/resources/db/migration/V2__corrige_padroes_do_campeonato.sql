-- V2: corrige os defaults de campeonato definidos na V1 (a V1 não é editada depois do primeiro deploy).
--
-- Na V1 o padrão "2/0/1" foi gravado como pontos_derrota_decisiva = 0 e pontos_derrota = 1, o que viola o
-- próprio CHECK ck_campeonato_pontuacao (derrota decisiva >= derrota). O padrão do PRD é vitória 2,
-- derrota 0 e derrota decisiva 1. A ordem de desempate também passa a usar os nomes do domínio.

ALTER TABLE campeonato ALTER COLUMN pontos_derrota_decisiva SET DEFAULT 1;
ALTER TABLE campeonato ALTER COLUMN pontos_derrota SET DEFAULT 0;
ALTER TABLE campeonato ALTER COLUMN ordem_desempate SET DEFAULT 'CONFRONTO_DIRETO,SALDO,JOGOS_VENCIDOS';
ALTER TABLE campeonato ALTER COLUMN tamanho_time SET DEFAULT 3;
