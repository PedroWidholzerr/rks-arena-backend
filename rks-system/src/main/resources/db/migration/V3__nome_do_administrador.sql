-- V3: nome de exibição do administrador (o painel o chama pelo nome e não pelo e-mail).
-- Opcional: sem nome, a tela mostra o e-mail.

ALTER TABLE usuario ADD COLUMN nome varchar(120);
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_nome CHECK (nome IS NULL OR length(btrim(nome)) > 0);
