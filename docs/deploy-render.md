# Deploy do backend no Render (free) com Supabase

## Serviço no Render

- Tipo: Web Service, ambiente **Docker**, repositório `rks-arena-backend`, branch `main`.
- Dockerfile na raiz do repositório (`Dockerfile`); contexto: raiz.
- Health check path: `/readyz`.
- O Dockerfile já fixa `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 -Xmx300m`. O `application.properties` liga `spring.main.lazy-initialization=true`, e o Flyway fica fora da inicialização preguiçosa.
- O Render injeta `PORT` e `RENDER_GIT_COMMIT` (o commit entra na versão devolvida por `/readyz`).

## Variáveis de ambiente (cadastradas no painel do Render, nunca no Git)

| Variável | Conteúdo |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://<host-do-session-pooler>:5432/postgres` (**session pooler**, IPv4) |
| `DB_USER` | Usuário do pooler (formato `postgres.<ref-do-projeto>`) |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo de pelo menos 32 caracteres; sem ele a aplicação não sobe |
| `SUPABASE_URL` | URL do projeto de produção |
| `SUPABASE_SERVICE_KEY` | Chave de serviço do Supabase (só no backend) |
| `ADMIN_EMAIL` | E-mail do primeiro administrador |
| `ADMIN_INITIAL_PASSWORD` | Senha inicial (troca obrigatória no primeiro login) |

## Verificação depois do deploy

1. `GET https://<serviço>.onrender.com/readyz` devolve `{"status":"UP","versao":"..."}`. Se o banco não responder, devolve 503.
2. `GET https://<serviço>.onrender.com/api/public/status` devolve `{"ok":true,"versao":"..."}`.
3. No banco de produção, a tabela `flyway_schema_history` mostra a `V1` aplicada com sucesso.
4. Erros 5xx aparecem no log do Render com `correlacaoId`, sem corpo de requisição nem segredo. A resposta traz o cabeçalho `X-Correlation-Id`.

## Cold start (medir três vezes)

Com o serviço dormindo (pelo menos 15 minutos sem requisição), cronometrar até o `/readyz` responder 200:

```
curl -s -o /dev/null -w "%{time_total}s\n" https://<serviço>.onrender.com/readyz
```

| Medição | Data | Tempo até responder 200 |
|---|---|---|
| 1 | pendente | pendente |
| 2 | pendente | pendente |
| 3 | pendente | pendente |

Expectativa do NFR-4: cerca de 1 minuto.
