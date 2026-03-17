# 🚀 Guia de Setup - Backend RKS Arena

Instruções passo a passo para configurar e executar o backend Java Spring Boot.

## 📋 Pré-requisitos

### Software Necessário

1. **Java 21 (JDK)**
   - Download: https://www.oracle.com/java/technologies/downloads/#java21
   - Ou: https://adoptium.net/
   - Verificar: `java -version`

2. **Maven 3.8.1+**
   - Download: https://maven.apache.org/download.cgi
   - Verificar: `mvn -version`

3. **PostgreSQL 12+**
   - Download: https://www.postgresql.org/download/
   - Portdefault: 5432

4. **Git** (opcional, para clonar repo)
   - Download: https://git-scm.com/

## 🔧 Instalação do PostgreSQL

### Windows
1. Download installer de https://www.postgresql.org/download/windows/
2. Execute e siga o instalador
3. **Anote a senha do usuário `postgres`**
4. Use porta padrão 5432
5. Install pgAdmin (opcional, para gerenciar DB)

### macOS
```bash
brew install postgresql
brew services start postgresql
```

### Linux (Ubuntu/Debian)
```bash
sudo apt-get install postgresql postgresql-contrib
sudo service postgresql start
```

## 🗄️ Criar Banco de Dados

### Via psql (linha de comando)
```bash
# Conectar como superuser
psql -U postgres

# Criar banco de dados
CREATE DATABASE rks_arena;

# Listar bancos
\l

# Sair
\q
```

### Via pgAdmin (interface gráfica)
1. Abra pgAdmin
2. Clique com direito em "Databases"
3. Create → Database
4. Nome: `rks_arena`
5. Save

## 📥 Setup do Projeto

### 1. Clone/Abra o Projeto
```bash
cd c:\RKS\backend\rks-system
# ou
cd /path/to/rks-system
```

### 2. Configurar Credenciais PostgreSQL

Edite `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rks_arena
spring.datasource.username=postgres
spring.datasource.password=SEU_PASSWORD_AQUI  # <-- Coloque a senha
```

### 3. Download de Dependências
```bash
mvn clean install
```

Isso vai:
- Baixar todas as dependências
- Compilar o projeto
- Preparar tudo para execução

## ▶️ Executar o Backend

### Opção 1: Maven CLI
```bash
mvn spring-boot:run
```

### Opção 2: Maven build + JAR
```bash
mvn clean package
java -jar target/rks-system-0.0.1-SNAPSHOT.jar
```

### Opção 3: IDE (IntelliJ IDEA)
1. Abra projeto em IntelliJ
2. Clique em Run → Run... (ou Shift+Alt+F10)
3. Selecione Application
4. Run

### Opção 4: VSCode + Extensions
1. Instale "Extension Pack for Java"
2. Clique em Run/Debug no topo do método main

## ✅ Verificar se está Rodando

Quando iniciado com sucesso, você verá:
```
Tomcat started on port(s): 8080 (http)
Started Application in X.XXX seconds

✅ Database populated with sample data!
🔐 Login credentials:
   Email: admin@example.com
   Password: admin123
🚀 API running at: http://localhost:8080
```

### Testar Endpoint
```bash
curl http://localhost:8080/api/auth/me
# Resposta esperada: HTTP 401 (não autenticado)
```

## 🔑 Primeiro Login

### Credenciais Padrão
- **Email**: admin@example.com
- **Password**: admin123

### Obter Token JWT
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin123"
  }'
```

Resposta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "...",
    "email": "admin@example.com",
    "name": "Admin User",
    "role": "ADMIN"
  }
}
```

## 🌐 Testar com Postman

1. Importe arquivo: `postman_collection.json` (se disponível)
2. Ou importe manualmente:

### Criar Coleção
- Name: RKS Arena API
- Base URL: `{{baseUrl}}`

### Variáveis
- `baseUrl`: `http://localhost:8080`
- `token`: adicionar após login

### Requests
```
POST /api/auth/login
GET /api/seasons
POST /api/teams
GET /api/players?teamId=id
...
```

## 🐛 Troubleshooting

### Erro: "Connection refused" (PostgreSQL)
**Problema**: PostgreSQL não está rodando

**Solução**:
```bash
# Windows
net start postgresql-x64-XX

# macOS
brew services start postgresql

# Linux
sudo service postgresql start
```

### Erro: "FATAL: password authentication failed"
**Problema**: Senha errada em application.properties

**Solução**:
1. Reset password PostgreSQL (Google: "reset postgres password [seu SO]")
2. Update `application.properties` com senha correta

### Erro: "Database does not exist"
**Problema**: Banco `rks_arena` não foi criado

**Solução**:
```bash
psql -U postgres
CREATE DATABASE rks_arena;
```

### Erro: "Port 8080 already in use"
**Problema**: Outra aplicação usando porta 8080

**Solução A**: Parar outra aplicação
```bash
# Windows - encontrar processo
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

**Solução B**: Mudar porta (application.properties)
```properties
server.port=3000
```

### Erro: "Maven not found"
**Problema**: Maven não está no PATH

**Solução**:
1. Confirme instalação: `mvn -version`
2. Windows: Adicione ao PATH: `C:\Program Files\Apache\apache-maven-3.x.x\bin`
3. Reinicie terminal/IDE

### Erro: Java version
**Problema**: Java < 21

**Solução**:
```bash
java -version  # Verificar
# Se < 21, download Java 21 e atualize JAVA_HOME
```

## 📊 Estrutura de Dados

### Diagrama de Relacionamentos
```
User (1) ←→ (n) Season
Season (1) ←→ (n) Championship
Championship (1) ←→ (n) Match
Championship (1) ←→ (n) Standing
Team (1) ←→ (n) Player
Team (1) ←→ (n) Match (como teamA/teamB)
Team (1) ←→ (n) Standing
Player (0..1) ← Match (mvp)
```

## 🔒 Segurança

### Senhas Padrão em DEV
⚠️ **IMPORTANTE**: Mude em produção!

- `jwt.secret` em `application.properties`
- Senha do admin
- Credenciais DB

### CORS
Frontend permitido:
- `http://localhost:5173` (dev frontend)
- `http://localhost:3000` (dev alternativo)

Adicione mais em `SecurityConfig.java` se necessário.

## 📚 Documentação API

Todos os endpoints estão documentados em [API_ENDPOINTS.md](../rks-arena/API_ENDPOINTS.md)

## 🚀 Próximos Passos

1. ✅ Backend rodando em http://localhost:8080
2. ✅ Frontend em http://localhost:5173 (já há hooks pronto)
3. ✅ Conecte frontend ao backend
4. ✅ Teste fluxos de CRUD
5. ✅ Implemente features adicionais

## 📞 Envolvimento Fronten-Backend

### Frontend (React 19 + TypeScript)
- Já tem hooks criados para cada entidade
- Está configurado para chamar `http://localhost:8080/api`
- Armazena token JWT em localStorage

### Backend (Spring Boot + Java 21)
- Implementa todo o swagger/documentação automática
- Cria/valida JWTs
- Calcula standings automaticamente
- Protege endpoints com @PreAuthorize

## ✨ Bom Desenvolvimento!

```
🎮 RKS Arena Backend está pronto para uso! 🎮
```
