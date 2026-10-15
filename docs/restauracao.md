# Backup e restauração

O banco gratuito (Supabase) pode ser pausado ou apagado. Por isso há três camadas de cópia:

1. **Backup semanal** (`.github/workflows/backup.yml`): `pg_dump -Fc` criptografado com `age`, guardado como artefato do repositório privado por **90 dias**.
2. **Exportação em JSON** (`GET /api/admin/export`, botão "Exportar dados" em Administradores): uma cópia legível que o administrador guarda fora do sistema. Não inclui hash de senha.
3. **Imagens**: ficam no Supabase Storage (bucket `rks-img`), fora do banco; o backup do banco guarda só as chaves.

## Preparar o backup (uma vez)

1. Gere o par de chaves `age` na sua máquina e **guarde a chave privada fora do repositório**:
   ```
   age-keygen -o chave-privada.txt
   ```
2. Cole a linha `Public key: age1...` em `backup/age-publica.txt`, no lugar de `AGE_CHAVE_PUBLICA_PENDENTE`, e faça commit. A chave pública pode ficar no Git.
3. No GitHub, em *Settings → Secrets and variables → Actions*, crie o segredo `BACKUP_DATABASE_URL`:
   `postgresql://USUARIO:SENHA@HOST:5432/postgres?sslmode=require` (use o **session pooler** do Supabase).
4. Rode o workflow uma vez à mão (*Actions → Backup semanal → Run workflow*) e confira o artefato `backup-rks`.

## Restaurar

Precisa de `age` e do cliente PostgreSQL 17 (`pg_restore`).

1. Baixe o artefato `backup-rks` mais recente no GitHub (*Actions → execução → Artifacts*) e descompacte o `.zip`.
2. Descriptografe com a chave privada:
   ```
   age -d -i chave-privada.txt -o rks.dump rks-AAAA-MM-DD.dump.age
   ```
3. Crie um banco **vazio** de destino (outro projeto Supabase ou um Postgres local) e restaure:
   ```
   pg_restore --no-owner --no-privileges --dbname "postgresql://USUARIO:SENHA@HOST:5432/postgres" rks.dump
   ```
4. Aponte o backend para o banco novo (`DATABASE_URL`, `DB_USER`, `DB_PASSWORD` no Render) e confira `GET /readyz`. O Flyway reconhece o histórico restaurado (`flyway_schema_history`) e não reaplica migrações.
5. Reenvie as imagens só se o bucket também se perdeu: as chaves restauradas apontam para objetos que não existem mais, e a tela mostra o avatar de iniciais (nunca imagem quebrada) até alguém enviar a imagem de novo.

## Ensaio de restauração

Seguir este documento de verdade num banco de teste e registrar o resultado aqui. **Pendente**: depende de a chave `age` e do segredo `BACKUP_DATABASE_URL` estarem configurados, e de um banco vazio de destino.

| Data | Quem | Origem do backup | Destino | Resultado | Duração |
|---|---|---|---|---|---|
| pendente | | | | | |

## Chaves de idempotência vencidas

A tabela `chave_idempotencia` guarda as respostas por 24 h. A limpeza das vencidas (`IdempotenciaService.limparVencidas`) roda junto da varredura semanal; as vencidas também são ignoradas e apagadas quando a mesma chave aparece de novo.
