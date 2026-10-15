# Rodar o sistema local e testar o fluxo

Este roteiro sobe o backend e o front na sua máquina e leva você pelo fluxo do administrador, de ponta a ponta. Ele usa uma **schema temporária** (`teste_local`) no mesmo banco do Supabase, para o que você criar **não misturar** com os dados reais. No fim, uma linha apaga tudo.

## Pré-requisitos

- Java 21 e Node **22** (o Node 18 do seu trabalho não serve para este front: com `nvm-windows`, `nvm use 22`).
- O arquivo `rks-arena-backend-main/.env` com `DATABASE_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET` (já existe na sua máquina).

## 1. Backend (terminal 1)

Dentro de `rks-arena-backend-main/rks-system`:

**PowerShell**

```powershell
Get-Content ..\.env | ForEach-Object { if ($_ -match '^([A-Z_]+)=(.*)$') { Set-Item "env:$($Matches[1])" $Matches[2] } }
$env:DATABASE_URL = "$env:DATABASE_URL&currentSchema=teste_local"
$env:SPRING_FLYWAY_DEFAULT_SCHEMA = "teste_local"
$env:SPRING_FLYWAY_SCHEMAS = "teste_local"
$env:SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA = "teste_local"
$env:ADMIN_EMAIL = "admin@local.teste"
$env:ADMIN_INITIAL_PASSWORD = "Senha-Inicial-Local-1"
$env:PORT = "8082"
.\mvnw spring-boot:run
```

**Git Bash**

```bash
set -a; . ../.env; set +a
export DATABASE_URL="${DATABASE_URL}&currentSchema=teste_local" SPRING_FLYWAY_DEFAULT_SCHEMA=teste_local SPRING_FLYWAY_SCHEMAS=teste_local SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA=teste_local
export ADMIN_EMAIL=admin@local.teste ADMIN_INITIAL_PASSWORD=Senha-Inicial-Local-1 PORT=8082
./mvnw spring-boot:run
```

Espere o log mostrar a V1 e a V2 aplicadas na schema `teste_local` e "Primeiro administrador criado". Confira em http://localhost:8082/readyz (deve responder `{"status":"UP",...}`).

## 2. Front (terminal 2)

Dentro de `rks-arena-main`:

```bash
npm ci
echo "VITE_API_URL=http://localhost:8082/api" > .env.local
npm run dev
```

Abra o endereço que o Vite mostrar (normalmente http://localhost:8080). O `.env.local` é ignorado pelo Git.

## 3. Roteiro de teste

Marque o que passou e anote o que estranhou (de preferência com print).

**Administrador**

1. Abra `/entrar` e entre com `admin@local.teste` e `Senha-Inicial-Local-1`. Deve levar direto a **Troque sua senha** (nenhuma outra tela admin abre antes).
2. Troque a senha (10 caracteres ou mais). Deve cair em **Pendências** ("Nada pendente. Tudo em dia.").
3. Tente uma senha curta e senhas diferentes na troca: as mensagens devem ser claras e o texto digitado deve ficar.

**Cadastros**

4. **Jogadores:** crie 6 Jogadores (ex.: `Ana#BR1` até `Fer#BR1`). Tente criar `ana#br1` de novo: deve avisar "Já cadastrado" com link para o perfil. Teste uma foto PNG pequena (com `SUPABASE_URL`/`SUPABASE_SERVICE_KEY` e o bucket `rks-img` só funciona se estiverem configurados; sem eles a mensagem deve ser clara, sem travar).
5. **Times:** crie 2 Times (ex.: `ALF` e `BET`). Tag repetida (`alf`) deve ser recusada.
6. **Elenco:** em cada Time, abra **Elenco**, escolha a Temporada (crie uma antes, passo 7, se não houver), adicione 3 titulares, marque 1 Capitão e salve. Teste salvar sem Capitão, com 2 titulares, e remover o Capitão sem trocar.
7. **Campeonatos:** crie uma Temporada e, nela, um Campeonato (melhor de 3, pontuação padrão). Abra o Campeonato e **vincule** os 2 Times.

**Partidas e resultado**

8. **Partidas:** escolha o Campeonato, **Agendar Partida** (ALF contra BET, com data e hora). Tente um link `http://` (deve ser recusado; `https://` passa).
9. Na Partida: **Iniciar partida**, depois **Lançar resultado**. O painel abre ao lado (no celular, sobe de baixo). Escolha um placar da lista (só aparecem 2×0, 2×1, 0×2 e 1×2), escolha um MVP e salve. O toast deve dizer "Resultado salvo… Classificação atualizada.".
10. Abra a página pública do Campeonato (`/campeonatos/…`): a **Classificação** deve mostrar os pontos certos (vitória 2, derrota decisiva 1, derrota 0). Confira à mão.
11. **Editar resultado:** mude o placar e confira a tabela de novo. **Anular resultado:** a Partida volta a Agendada e a tabela volta a zero.
12. Volte a **Campeonatos → o Campeonato** com uma Partida finalizada: as regras (melhor de N, pontos) devem aparecer **bloqueadas**, com o aviso e o link para as Partidas.
13. **Administradores:** crie 2 Administradores (a senha temporária aparece uma vez); o 4º deve ser recusado; desative um; tente desativar o último ativo (deve recusar). Teste **Exportar dados**.

**Aparência e acessibilidade**

14. Alterne o tema (**Claro**, **Escuro**, **Sistema**) e recarregue: a escolha fica.
15. Diminua a janela para ~360 px: a tabela mantém posição, time, J, V, D, SJ e Pts, e "Ver detalhes" mostra Jogos a favor e contra. O menu vira gaveta.
16. Navegue só pelo teclado (Tab): o primeiro foco é "Pular para o conteúdo" e o anel de foco aparece em todos os controles.
17. Pare o backend (Ctrl+C) e recarregue uma página pública: deve aparecer **"Acordando o servidor…"** depois de uns 3 s (aviso âmbar, nunca erro vermelho). Religue o backend e a página deve se recuperar sozinha.

## 4. Limpar

Pare os dois processos e apague a schema temporária (no Git Bash, na pasta `rks-system`, com o `.env` carregado como acima):

```bash
java -cp ~/.m2/repository/org/postgresql/postgresql/42.7.10/postgresql-42.7.10.jar scripts/ApagarSchema.java teste_local
```

Se preferir, apague pelo painel do Supabase (SQL Editor): `DROP SCHEMA teste_local CASCADE;`. **Nunca** rode isso com o nome `public`.
