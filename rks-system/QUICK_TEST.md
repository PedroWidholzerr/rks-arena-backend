# 🧪 Quick Test - Validar Backend

Teste rápido para validar se o backend está funcionando corretamente.

## ✅ Pré-requisitos

- Backend rodando em `http://localhost:8080`
- PostgreSQL com banco `rks_arena` criado
- Postman ou cURL instalado

## 📝 Testes via cURL

### 1. ✅ Health Check
```bash
curl http://localhost:8080/api/auth/me
```
**Esperado**: HTTP 401 Unauthorized (sem token)

### 2. ✅ Login (Obter Token)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'
```
**Esperado**: HTTP 200 + token JWT + dados do usuário

**Salve o token para próximos testes:**
```bash
TOKEN="seu_token_aqui"
```

### 3. ✅ Listar Seasons
```bash
curl http://localhost:8080/api/seasons
```
**Esperado**: HTTP 200 + array com 2 seasons

### 4. ✅ Criar Season (Protegido)
```bash
curl -X POST http://localhost:8080/api/seasons \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Season 3","year":2026}'
```
**Esperado**: HTTP 201 + dados da nova season

### 5. ✅ Listar Teams
```bash
curl http://localhost:8080/api/teams
```
**Esperado**: HTTP 200 + array com 3 teams

### 6. ✅ Listar Players de um Time
```bash
# Pegue um teamId da resposta anterior
TEAM_ID="team_id_aqui"

curl "http://localhost:8080/api/players?teamId=$TEAM_ID"
```
**Esperado**: HTTP 200 + array com players do time

### 7. ✅ Listar Matches
```bash
curl http://localhost:8080/api/matches
```
**Esperado**: HTTP 200 + array com 3 matches

### 8. ✅ Listar Standings de um Campeonato
```bash
# Pegue um championshipId
CHAMPIONSHIP_ID="championship_id_aqui"

curl "http://localhost:8080/api/standings?championshipId=$CHAMPIONSHIP_ID"
```
**Esperado**: HTTP 200 + array com standings ordenado por pontos

### 9. ✅ Atualizar Match (Auto-update Standings)
```bash
# Pegue um matchId
MATCH_ID="match_id_aqui"

curl -X PUT "http://localhost:8080/api/matches/$MATCH_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scoreA": 3,
    "scoreB": 1,
    "comment": "Updated scores"
  }'
```
**Esperado**: HTTP 200 + match atualizado
**Verifique**: Standings foram atualizados automaticamente!

### 10. ✅ Deletar Season (Teste Cascata)
```bash
# Pegue um seasonId recém-criado
SEASON_ID="new_season_id"

curl -X DELETE "http://localhost:8080/api/seasons/$SEASON_ID" \
  -H "Authorization: Bearer $TOKEN"
```
**Esperado**: HTTP 204 No Content

---

## 🖥️ Testes via Postman

### Setup
1. Abra Postman
2. File → Import → postman_collection.json
3. Adicione variável `baseUrl = http://localhost:8080/api`

### Execução
1. **Auth** → Run "Login"
   - Salve token em `{{token}}`
   
2. **Seasons** → Run "Get All Seasons"
   - Verifique 2+ seasons existem
   
3. **Teams** → Run "Get All Teams"
   - Verifique 3 teams existem
   
4. **Players** → Run "Get Players by Team"
   - Use teamId de um time
   
5. **Matches** → Run "Get All Matches"
   - Verifique 3 matches

---

## 🔍 Validações Importantes

### ✅ Autenticação JWT
```bash
# Com token válido - sucesso
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/seasons

# Sem token - 401
curl http://localhost:8080/api/seasons

# Token inválido - 401
curl -H "Authorization: Bearer invalid" http://localhost:8080/api/seasons
```

### ✅ Proteção de Endpoints
```bash
# POST sem ADMIN - 403 (se usuário não for admin)
curl -X POST http://localhost:8080/api/seasons \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","year":2026}'

# Esperado: HTTP 201 (admin) ou 403 (user)
```

### ✅ Validação de Dados
```bash
# Tag duplicada - erro
curl -X POST http://localhost:8080/api/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Duplicate","tag":"SHW"}'

# Esperado: 400 Bad Request
```

### ✅ Standings Automático
```bash
# 1. Criar match com scoreA=1, scoreB=0
#    Team A deve ter +1 win e +3 pontos

# 2. Atualizar match para scoreA=0, scoreB=1
#    Team A perde -1 win e -3 pontos
#    Team B ganha +1 win e +3 pontos

# 3. Deletar match
#    Ambos times voltam aos valores originais
```

---

## 📊 Verificar Dados no PostgreSQL

```sql
-- Conectar
psql -U postgres -d rks_arena

-- Ver usuários
SELECT email, role FROM users;

-- Ver seasons
SELECT name, year FROM seasons;

-- Ver times
SELECT name, tag FROM teams;

-- Ver jogadores
SELECT nickname, tag FROM players;

-- Ver partidas
SELECT * FROM matches;

-- Ver standings
SELECT c.name, t.name, s.wins, s.losses, s.points
FROM standings s
JOIN championship c ON s.championship_id = c.id
JOIN team t ON s.team_id = t.id
ORDER BY s.points DESC;

-- Sair
\q
```

---

## 🐛 Troubleshooting

### Erro: "Connection refused"
```
❌ Backend não está rodando
✅ Execute: mvn spring-boot:run
```

### Erro: "Access Denied"
```
❌ Token expirado ou role insuficiente
✅ Faça login novamente para obter novo token
```

### Erro: "Stand not Found"
```
❌ ID inválido ou não existe
✅ Verifique o ID da requisição anterior
```

### Erro: "Email already exists"
```
❌ Tentando registrar email duplicado
✅ Use outro email ou delete usuário anterior
```

### Erro: "Foreign key constraint"
```
❌ Tentando referenciar entidade que não existe
✅ Crie primeiro a entidade (ex: Season antes de Championship)
```

---

## 🎯 Checklist de Validação

- [ ] Backend inicia sem erros
- [ ] Login retorna token JWT
- [ ] GET /seasons retorna 2+ seasons
- [ ] GET /teams retorna 3+ times
- [ ] GET /players retorna 3+ jogadores
- [ ] GET /matches retorna 3+ partidas
- [ ] POST /seasons requer autenticação
- [ ] PUT /matches atualiza standings
- [ ] DELETE /seasons cascata funciona
- [ ] CORS está permitindo frontend
- [ ] Dados iniciais foram criados
- [ ] Token expira e pode ser renovado

---

## 📈 Performance (Opcional)

```bash
# Medir tempo de resposta
time curl http://localhost:8080/api/seasons

# Listar todos (sem filtro)
curl http://localhost:8080/api/matches

# Filtrado
curl "http://localhost:8080/api/matches?championshipId=c1"
```

**Esperado**: Respostas < 200ms

---

## ✨ Próximas Etapas

1. ✅ Backend validado
2. → Conectar frontend React
3. → Testar CRUD integrado
4. → Deploy em produção
5. → Monitoramento em produção

---

**Se todos os testes passarem, o backend está 100% funcional!** ✅

```
./run_test.sh  (script não criado, use cURL manualmente)
```

## 📞 Troubleshooting Final

Se algum teste falhar:
1. Verifique logs do backend (console)
2. Verifique PostgreSQL está rodando
3. Verifique credenciais em application.properties
4. Tente: `mvn clean install && mvn spring-boot:run`
5. Confirme endpoint: http://localhost:8080

---

**Sucesso! 🎉 seu backend está pronto!**
