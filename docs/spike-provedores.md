# Spikes do Epic 1: provedores, Flyway e cotas

Este documento registra os resultados dos spikes 1.1, 1.2 e 1.3. Cada `[ASSUMPTION]` do Spine termina como **confirmado** ou **refutado**. Se algum for refutado, a história para e pergunta, sem trocar de provedor sozinha.

Legenda do estado: **pendente** (ainda não executado), **aguardando o mantenedor** (exige login ou painel de provedor), **confirmado**, **refutado**.

## 1.2 Starter do Flyway no Spring Boot 4.0.3

Estado: **confirmado** em 2026-10-15.

Dependências candidatas, já no `pom.xml`:

| Artefato | Versão | Escopo | Observação |
|---|---|---|---|
| `org.springframework.boot:spring-boot-starter-flyway` | gerenciada pelo Boot 4.0.3 | compile | No Boot 4 o `flyway-core` sozinho não ativa a autoconfiguração; é preciso o starter. |
| `org.flywaydb:flyway-database-postgresql` | gerenciada pelo Boot 4.0.3 | runtime | Módulo do driver PostgreSQL, exigido pelo Flyway 10 ou superior. |
| `org.testcontainers:testcontainers-junit-jupiter` | gerenciada pelo Boot 4.0.3 | test | Testcontainers 2.x: os módulos ganharam o prefixo `testcontainers-`. |
| `org.testcontainers:testcontainers-postgresql` | gerenciada pelo Boot 4.0.3 | test | Classe `org.testcontainers.postgresql.PostgreSQLContainer`. |

Pontos a conferir na execução:

- O Spine lista Testcontainers "1.x", mas o BOM do Boot 4 gerencia a linha 2.x. Se o BOM não gerenciar essas versões, registrar a versão exata aqui.
- Registrar a versão resolvida (`./mvnw dependency:tree | grep -i -E "flyway|testcontainers"`).
- Prova do spike: aplicar uma migração vazia `V0__spike.sql` em Postgres local ou no Supabase de teste e ver `./mvnw test` passar. A `V0` é descartada e não entra na `main`; quem prova o starter na `main` é a `V1__base.sql`, via `MigracaoV1IntegracaoTest`.

Resultado `[ASSUMPTION: nome do starter do Flyway]`: **confirmado**. Com `spring-boot-starter-flyway` e `flyway-database-postgresql`, a aplicação subiu contra o projeto Supabase de teste (session pooler) e o Flyway aplicou `1 - base` ("Successfully applied 1 migration"). O `./mvnw test` sem Docker passa (8 testes). Versões resolvidas pelo BOM do Boot 4.0.3; ver `./mvnw dependency:tree`. Testcontainers 2.x compilou (`testcontainers-junit-jupiter`, `testcontainers-postgresql`), mas a execução do `MigracaoV1IntegracaoTest` aguarda o Docker Desktop ligado.

Observações do projeto Supabase de teste:

- A conexão direta (`db.<ref>.supabase.co`) só resolve em IPv6 e não foi alcançável nesta máquina; o **session pooler** (`aws-1-sa-east-1.pooler.supabase.com:5432`, usuário `postgres.<ref>`) funcionou, como o Spine prevê.
- `[ASSUMPTION: PostgreSQL 17]`: **confirmado**, versão 17.11 (lida no log do Flyway).
- A V1 já está aplicada no projeto de teste; nunca editar a V1, só migração nova.

## 1.1 Provedores gratuitos sem cartão

Estado: **aguardando o mantenedor** (criar as contas e preencher a tabela).

| Item | Como verificar | Resultado |
|---|---|---|
| Render free: serviço Docker sem cartão | Criar o serviço Docker | pendente |
| Render free: RAM, horas por mês, tempo de repouso, banda | Anotar do painel | pendente |
| Supabase free: 2 projetos (produção e teste) sem cartão `[ASSUMPTION]` | Criar os dois projetos | **refutado** nesta conta: só é possível 1 projeto (informado pelo mantenedor em 2026-10-15). Ver "Decisão: projeto Supabase único" abaixo. |
| Supabase free: limites de banco, Storage e banda | Anotar do painel | pendente |
| PostgreSQL 17 disponível no Supabase `[ASSUMPTION]` | Log do Flyway | **confirmado** (17.11, projeto de teste) |
| Turnstile: site criado sem cartão | Criar o site | pendente |
| Vercel Hobby: build com Node 22.x `[ASSUMPTION]` | Configuração do projeto | pendente |
| Node 22.x disponível para o build | Painel da Vercel | pendente |

## 1.3 Cota do GitHub Actions e pausa do Supabase

Estado: **aguardando o mantenedor** (depende do CI da história 1.9 e do projeto Supabase).

- Workflow de teste com `pg_dump` e `curl /readyz` no repositório privado: pendente.
- Minutos gastos por execução: pendente.
- Cota mensal do plano: pendente.
- **Data de início da observação da pausa do Supabase:** a preencher quando o primeiro acesso ao banco for feito.
- Conclusão ("a conexão semanal conta como atividade?"): só depois de pelo menos 8 dias da data de início. **Esta história fica aberta até lá.**
- Se a conexão não contar: criar o item "usar `aquecer.yml` semanal" na história 2.32, sem mudar nenhuma AD.

## Decisão: projeto Supabase único

Suposição refutada: a conta só permite **1 projeto Supabase** (o Spine previa 2, produção e teste). Decisão do mantenedor em 2026-10-15: o projeto existente (hoje usado como "teste") passa a ser também o de **produção**. Nenhuma AD muda.

Consequências e cuidados:

- Os testes de integração do CI e locais usam **Testcontainers** (Postgres descartável) e nunca o Supabase, então não precisam de um segundo projeto.
- O Flyway já aplicou a `V1` neste projeto. O Render vai reaproveitar o mesmo banco: a `V1` não roda de novo, e o histórico fica em `flyway_schema_history`.
- Sem banco de teste separado, nenhuma execução manual (scripts, spikes) deve apontar para este banco depois do corte (história 2.34). Antes disso ele não tem dado de produto.
- O backup semanal (NFR-5, `pg_dump`) passa a proteger o único banco que existe. O backup deve ser feito antes de qualquer experimento com dados reais.
- Se for preciso um segundo ambiente no futuro, opções sem custo: segundo projeto em outra conta ou organização, ou um schema separado no mesmo projeto. Qualquer das duas exige nova decisão.
