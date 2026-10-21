# CampZone: Backend

API do CampZone (SaaS para organizar campeonatos). Java 21, Spring Boot 4.1, Maven, PostgreSQL.
Regras de arquitetura e padrões: veja `CLAUDE.md`.

O frontend fica no repositório `campzone-frontend` (`github.com/Attonic/campzone-frontend`).

## Pré-requisitos (Fedora)

- **JDK 21** (ex.: Temurin). Se o `java` padrão da máquina não for o 21, aponte o `JAVA_HOME`:

  ```bash
  export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk
  export PATH="$JAVA_HOME/bin:$PATH"
  java -version   # deve mostrar 21
  ```

- **Docker** com o plugin `docker compose` (o usuário precisa rodar `docker` sem sudo). Também é usado pelos
  testes (Testcontainers sobe um PostgreSQL descartável).

## Rodando em desenvolvimento (perfil `dev`)

O perfil `dev` é o padrão: sem informar perfil, a aplicação usa `application-dev.yml`, que conecta no
PostgreSQL do `docker-compose.yml` em `localhost:5433` (usuário, senha e banco `campzone`).

1. Suba o PostgreSQL (não precisa de `.env`; copie `.env.example` para `.env` só se quiser mudar algo):

   ```bash
   docker compose up -d
   docker compose ps   # aguarde "healthy"
   ```

2. Suba a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

   A API fica em `http://localhost:8080`. Rotas públicas ficam em `/api/v1/public/**`; qualquer outra rota
   exige autenticação (responde 401 até a tarefa de JWT). Erros saem como `ProblemDetail` (RFC 9457).

Para parar o banco: `docker compose down` (acrescente `-v` para apagar os dados).

A porta 5433 evita conflito com um PostgreSQL já instalado na 5432. Para mudar, defina `POSTGRES_PORT` no `.env`
e aponte a aplicação com `DB_URL=jdbc:postgresql://localhost:<porta>/campzone`. `DB_USER` e `DB_PASSWORD`
também sobrescrevem o `dev`.

### IntelliJ IDEA

- Em *Project Structure > SDK*, use o `temurin-21` (JDK 21).
- Rode a classe `CampzoneApplication` sem configurar perfil: cai no `dev`. Requer o `docker compose up -d`.
- Alternativa sem docker-compose: rode `TestCampzoneApplication` (em `src/test`), que sobe um PostgreSQL
  descartável via Testcontainers (precisa do Docker rodando, mas não do compose).

## Produção (perfil `prod`)

O perfil `prod` (`application-prod.yml`) não tem defaults para o banco. Variáveis **obrigatórias**; sem elas a
aplicação não sobe e informa quais faltam:

| Variável | Descrição |
|---|---|
| `DB_URL` | URL JDBC, ex.: `jdbc:postgresql://host:5432/campzone` |
| `DB_USER` | usuário do banco |
| `DB_PASSWORD` | senha do banco |

Opcionais: `PORT` (padrão 8080), `DB_POOL_MAX_SIZE` (padrão 10), `DB_POOL_MIN_IDLE` (padrão 2).

```bash
./mvnw -DskipTests package
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://host:5432/campzone DB_USER=... DB_PASSWORD=... \
  java -jar target/campzone-0.0.1-SNAPSHOT.jar
```

Em prod o log é INFO, `show-sql` fica desligado e as respostas de erro não incluem mensagem nem stacktrace.

### Configuração

O schema é gerado pelo Hibernate (`ddl-auto=update`); não há migrations. Nenhum segredo é versionado.

## Testes

```bash
./mvnw verify
```

Inclui `contextLoads` com Testcontainers (PostgreSQL), então o Docker precisa estar rodando.

## CI

`.github/workflows/ci.yml` roda `./mvnw -B verify` com Temurin 21 em pull requests e em push na `main`.
