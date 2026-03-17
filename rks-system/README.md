# RKS Arena Backend - Spring Boot API

Backend Java Spring Boot para o sistema de gerenciamento de arenas/torneios RKS Arena.

## 🚀 Começando

### Pré-requisitos

- **Java 21** ou posterior
- **Maven 3.8+**
- **PostgreSQL 12+**
- **Git**

### Instalação

1. **Clone o repositório**
```bash
git clone <repo-url>
cd rks-system
```

2. **Configure o PostgreSQL**

Crie um banco de dados:
```sql
CREATE DATABASE rks_arena;
```

(Opcional) Se usar usuário diferente, atualize em `application.properties`:
```properties
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
```

3. **Instale as dependências**
```bash
mvn clean install
```

4. **Execute a aplicação**
```bash
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8080`

## Deploy no Railway

Se o deploy for feito a partir da pasta `backend`, o Railway/Railpack não encontra o `pom.xml` porque o projeto Java está dentro de `rks-system`.

Para resolver isso, este repositório inclui um `Dockerfile` na raiz de `backend`.

### Variáveis de ambiente recomendadas

```properties
PORT=8081
PGHOST=<host-do-postgres>
PGPORT=5432
PGDATABASE=<nome-do-banco>
PGUSER=<usuario>
PGPASSWORD=<senha>
JWT_SECRET=<segredo-forte>
```

O backend também aceita `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` caso você prefira passar a URL JDBC completa.

Também aceita `DATABASE_URL` e `POSTGRES_URL` no formato `postgres://...` ou `postgresql://...`, convertendo automaticamente para JDBC.

### Healthcheck

Use o endpoint abaixo no provedor de deploy:

```text
/actuator/health
```

Esse endpoint está liberado sem autenticação para que o Railway consiga validar a aplicação.

## 📁 Estrutura do Projeto

```
src/main/java/com/rks/
├── controller/          # REST Controllers (endpoints)
├── service/            # Lógica de negócio
├── repository/         # Spring Data JPA repositories
├── entity/            # Entidades JPA
├── dto/               # Data Transfer Objects
├── security/          # Segurança JWT
└── config/            # Configurações do Spring

src/main/resources/
└── application.properties  # Configurações da aplicação
```

## 🔐 Autenticação JWT

### Fluxo de Autenticação

1. **Login**
   ```bash
   POST /api/auth/login
   {
     "email": "admin@example.com",
     "password": "password"
   }
   ```
   Resposta:
   ```json
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "user": {
       "id": "usr-1",
       "email": "admin@example.com",
       "name": "Admin User",
       "role": "ADMIN"
     }
   }
   ```

2. **Usar Token**
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

3. **Refresh Token**
   ```bash
   POST /api/auth/refresh
   Headers: Authorization: Bearer {token}
   ```

### Usuários Padrão (dados de seed)

Você precisa adicionar dados iniciais. Veja a seção "Populando Dados Iniciais".

## 📚 Endpoints Disponíveis

### Autenticação
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Registrar novo usuário
- `POST /api/auth/refresh` - Renovar token
- `GET /api/auth/me` - Obter dados do usuário autenticado

### Seasons (Temporadas)
- `GET /api/seasons` - Listar todas
- `GET /api/seasons/{id}` - Obter uma
- `POST /api/seasons` - Criar
- `PUT /api/seasons/{id}` - Atualizar
- `DELETE /api/seasons/{id}` - Deletar

### Championships (Campeonatos)
- `GET /api/championships` - Listar todas
- `GET /api/championships?seasonId={id}` - Filtrar por temporada
- `GET /api/championships/{id}` - Obter uma
- `POST /api/championships` - Criar
- `PUT /api/championships/{id}` - Atualizar
- `DELETE /api/championships/{id}` - Deletar

### Teams (Times)
- `GET /api/teams` - Listar todos
- `GET /api/teams/{id}` - Obter um
- `POST /api/teams` - Criar
- `PUT /api/teams/{id}` - Atualizar
- `POST /api/teams/{id}/logo` - Upload de logo
- `DELETE /api/teams/{id}` - Deletar

### Players (Jogadores)
- `GET /api/players` - Listar todos
- `GET /api/players?teamId={id}` - Filtrar por time
- `GET /api/players/{id}` - Obter um
- `POST /api/players` - Criar
- `PUT /api/players/{id}` - Atualizar
- `DELETE /api/players/{id}` - Deletar

### Matches (Partidas)
- `GET /api/matches` - Listar todas
- `GET /api/matches?championshipId={id}` - Filtrar por campeonato
- `GET /api/matches?teamId={id}` - Filtrar por time
- `GET /api/matches/{id}` - Obter uma
- `POST /api/matches` - Criar
- `PUT /api/matches/{id}` - Atualizar
- `DELETE /api/matches/{id}` - Deletar

### Standings (Classificações)
- `GET /api/standings?championshipId={id}` - Listar por campeonato *(obrigatório)*
- `GET /api/standings/{id}` - Obter uma
- `POST /api/standings` - Criar
- `PUT /api/standings/{id}` - Atualizar
- `DELETE /api/standings/{id}` - Deletar

## ⚙️ Configuração

### Alterar Porta
Em `application.properties`:
```properties
server.port=3000
```

### Alterar Banco de Dados
Em `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://seu-host:5432/seu-banco
spring.datasource.username=seu-usuario
spring.datasource.password=sua-senha
```

### JWT Secret
Altere em `application.properties`:
```properties
jwt.secret=sua-secret-chave-super-segura
jwt.expiration=86400000  # 24 horas em ms
```

⚠️ **IMPORTANTE**: Mude isso em produção! Use uma chave bem forte.

### CORS
Adicione mais origens em `SecurityConfig.java`:
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:5173",
    "http://localhost:3000",
    "https://seu-dominio.com"
));
```

## 🗄️ Populando Dados Iniciais

Você pode criar um `CommandLineRunner` para adicionar dados iniciais:

```java
@Component
public class DataLoader {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = User.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Admin User")
                    .role(User.UserRole.ADMIN)
                    .build();
                userRepository.save(admin);
            }
        };
    }
}
```

Coloque este arquivo em `src/main/java/com/rks/config/DataLoader.java`.

## 🔍 Testando com cURL

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'
```

### Criar Season
```bash
curl -X POST http://localhost:8080/api/seasons \
  -H "Authorization: Bearer TOKEN_AQUI" \
  -H "Content-Type: application/json" \
  -d '{"name":"Season 1","year":2025}'
```

### Listar Champions
```bash
curl http://localhost:8080/api/championships \
  -H "Authorization: Bearer TOKEN_AQUI"
```

## 🐛 Troubleshooting

### Erro: "Connection refused"
- Certifique-se que PostgreSQL está rodando
- Verifique as credenciais em `application.properties`

### Erro: "Invalid token"
- Token pode estar expirado (padrão 24 horas)
- Execute `/api/auth/refresh` para renovar

### Erro: "Access Denied"
- Usuário não tem permissão ADMIN
- Importe um usuário com role ADMIN

## 📦 Build para Produção

```bash
mvn clean package -DskipTests
java -jar target/rks-system-0.0.1-SNAPSHOT.jar
```

## 🤝 Integração com Frontend

Configure seu frontend para usar:
```javascript
const API_URL = 'http://localhost:8080/api';
```

Token JWT é armazenado em localStorage e enviado em cada requisição:
```javascript
headers.Authorization = `Bearer ${token}`;
```

## 📝 Licença

Proprietário - RKS Arena © 2025

## 📧 Contato

Para dúvidas ou sugestões, contate o time de desenvolvimento.
