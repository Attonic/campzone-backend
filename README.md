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

## Rodando localmente

1. Variáveis do banco (opcional, os defaults já funcionam):

   ```bash
   cp .env.example .env
   ```

2. Suba o PostgreSQL:

   ```bash
   docker compose up -d
   docker compose ps   # aguarde "healthy"
   ```

3. Crie o perfil local copiando o exemplo. O Spring Boot lê `./config/` do diretório de execução, e o
   `application-local.yml` é ignorado pelo git (e fica fora do jar):

   ```bash
   cp config/application-local.example.yml config/application-local.yml
   ```

4. Suba a aplicação com o perfil `local`:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

   A API fica em `http://localhost:8080`. Rotas públicas ficam em `/api/v1/public/**`; qualquer outra rota
   exige autenticação (responde 401 até a tarefa de JWT). Erros saem como `ProblemDetail` (RFC 9457).

Se a porta 5432 já estiver em uso na máquina, defina `POSTGRES_PORT=5433` no `.env` e ajuste a `url` em `config/application-local.yml` (ou rode com
`DB_URL=jdbc:postgresql://localhost:5433/campzone`).

Para parar: `docker compose down` (acrescente `-v` para apagar os dados).

### Configuração

O datasource é lido das variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD`, com defaults para o compose local
(`jdbc:postgresql://localhost:5432/campzone`, `campzone`, `campzone`). O schema é gerado pelo Hibernate
(`ddl-auto=update`); não há migrations.

## Testes

```bash
./mvnw verify
```

Inclui `contextLoads` com Testcontainers (PostgreSQL), então o Docker precisa estar rodando.

## CI

`.github/workflows/ci.yml` roda `./mvnw -B verify` com Temurin 21 em pull requests e em push na `main`.
