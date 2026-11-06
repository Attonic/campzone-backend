# CampZone: Backend

> Parte do CampZone, desenvolvido por agentes de IA com revisão independente a cada tarefa. O processo, as specs e as decisões de arquitetura estão em [Attonic/campzone](https://github.com/Attonic/campzone).

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

O perfil `dev` usa `application-dev.yml`, que conecta no PostgreSQL do `docker-compose.yml` em `localhost:5433`
(usuário, senha e banco `campzone`) e traz um segredo JWT e contas de teste públicos.

**Não existe perfil padrão** (ADR 0004): sem `SPRING_PROFILES_ACTIVE` a aplicação não sobe e avisa "Nenhum perfil
ativo: defina SPRING_PROFILES_ACTIVE=dev (desenvolvimento) ou prod.". Assim um deploy que esquecesse o perfil `prod`
nunca cairia no `dev` por engano. O `./mvnw spring-boot:run` e as configurações de execução do IntelliJ já usam `dev`.

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
   exige um JWT (veja "Autenticação" abaixo). Erros saem como `ProblemDetail` (RFC 9457).

### Autenticação e contas de desenvolvimento

Login e cadastro são públicos (`POST /api/v1/public/auth/login` e `/api/v1/public/auth/cadastro`) e devolvem um JWT
(`Authorization: Bearer <token>`, validade padrão de 8 horas, sem refresh token). `GET /api/v1/auth/me` devolve o
usuário do token. Há dois papéis (ADR 0002): `ADMIN_PLATAFORMA` (só `/api/v1/admin/**`, sem organização) e
`ORGANIZADOR` (rotas da própria organização).

Em `dev`, a aplicação cria no primeiro start (idempotente) estas contas, definidas em `application-dev.yml`. Elas
**só existem em dev e não são segredo**:

| Conta | E-mail | Senha | Papel |
|---|---|---|---|
| Admin da plataforma | `admin@campzone.dev` | `admin12345` | `ADMIN_PLATAFORMA` |
| Organizador demo (org "Organização Demo", plano `GRATIS`) | `organizador@campzone.dev` | `organizador123` | `ORGANIZADOR` |

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/public/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"organizador@campzone.dev","senha":"organizador123"}' | jq -r .token)
curl -s localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
```

### Área do admin da plataforma (somente leitura)

Só o `ADMIN_PLATAFORMA` acessa (o organizador recebe 403, sem token 401). É o único lugar que consulta entre
organizações (ADR 0002):

- `GET /api/v1/admin/resumo`: `totalOrganizacoes`, `totalUsuarios`, `totalCampeonatos` e `campeonatosAtivos`
  (`RASCUNHO` ou `EM_ANDAMENTO`).
- `GET /api/v1/admin/organizacoes?pagina=0&tamanho=20&busca=texto`: organizações da mais nova para a mais antiga, com
  plano e contagens. `pagina` >= 0 (padrão 0), `tamanho` de 1 a 100 (padrão 20), `busca` com até 100 caracteres
  (nome contendo o texto, sem diferenciar maiúsculas). Fora dos limites devolve 400.

```bash
ADMIN=$(curl -s -X POST localhost:8080/api/v1/public/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@campzone.dev","senha":"admin12345"}' | jq -r .token)
curl -s localhost:8080/api/v1/admin/resumo -H "Authorization: Bearer $ADMIN"
curl -s "localhost:8080/api/v1/admin/organizacoes?busca=liga" -H "Authorization: Bearer $ADMIN"
```

Para parar o banco: `docker compose down` (acrescente `-v` para apagar os dados).

A porta 5433 evita conflito com um PostgreSQL já instalado na 5432. Para mudar, defina `POSTGRES_PORT` no `.env`
e aponte a aplicação com `DB_URL=jdbc:postgresql://localhost:<porta>/campzone`. `DB_USER` e `DB_PASSWORD`
também sobrescrevem o `dev`.

### IntelliJ IDEA

- Em *Project Structure > SDK*, use o `temurin-21` (JDK 21).
- Use a configuração de execução versionada **CampzoneApplication (dev)** (em `.run/`, aparece sozinha na lista de
  execuções): ela roda `CampzoneApplication` com `-Dspring.profiles.active=dev`. Requer o `docker compose up -d`.
  Se rodar a classe `CampzoneApplication` direto, sem perfil, a aplicação não sobe (veja acima).
- Alternativa sem docker-compose: **TestCampzoneApplication (dev)** (também em `.run/`) sobe um PostgreSQL
  descartável via Testcontainers (precisa do Docker rodando, mas não do compose).
- Pelo Maven: `./mvnw spring-boot:run` usa `dev`; para outro perfil, `-Dspring-boot.run.profiles=<perfil>`.
- Por jar: `SPRING_PROFILES_ACTIVE=dev java -jar target/campzone-0.0.1-SNAPSHOT.jar` (o jar nunca assume perfil).

## Produção (perfil `prod`)

O perfil `prod` (`application-prod.yml`) não tem defaults para o banco. Variáveis **obrigatórias**; sem elas a
aplicação não sobe e informa quais faltam:

| Variável | Descrição |
|---|---|
| `DB_URL` | URL JDBC, ex.: `jdbc:postgresql://host:5432/campzone` |
| `DB_USER` | usuário do banco |
| `DB_PASSWORD` | senha do banco |
| `JWT_SECRET` | segredo HMAC-SHA256 do JWT, mínimo 32 bytes (gere com `openssl rand -base64 48`) |
| `ADMIN_EMAIL` | e-mail do admin da plataforma, criado no primeiro start se não existir |
| `ADMIN_SENHA` | senha inicial do admin, mínimo 12 caracteres (nunca é sobrescrita depois de criado) |

As três `DB_*` são exigidas de propósito: definir só `SPRING_DATASOURCE_*` não basta, porque o
`application-prod.yml` lê apenas as variáveis `DB_*`.

Opcionais: `PORT` (padrão 8080), `DB_POOL_MAX_SIZE` (padrão 10), `DB_POOL_MIN_IDLE` (padrão 2). A validade do token
(`campzone.jwt.expiracao`, padrão `PT8H`) pode ser ajustada por propriedade.

```bash
./mvnw -DskipTests package
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://host:5432/campzone DB_USER=... DB_PASSWORD=... \
  JWT_SECRET=... ADMIN_EMAIL=... ADMIN_SENHA=... java -jar target/campzone-0.0.1-SNAPSHOT.jar
```

Em prod o log é INFO, `show-sql` fica desligado e as respostas de erro não incluem mensagem nem stacktrace.

### Configuração

O schema é gerado pelo Hibernate (`ddl-auto=update`); não há migrations. Nenhum segredo é versionado.

## Testes

```bash
./mvnw verify
```

Os testes usam Testcontainers (um PostgreSQL descartável), não o banco do `docker-compose.yml`; por isso o Docker
precisa estar rodando, mas o compose não. Os testes de perfis ignoram variáveis de ambiente como `DB_URL`.

### Cobertura (JaCoCo)

O `verify` gera o relatório em `target/site/jacoco/index.html` e **falha** se o pacote `motor` (com subpacotes) ficar
abaixo de 90% de cobertura de linhas ou de ramos. A regra vale só para o `motor`; os outros pacotes aparecem no
relatório, mas não são cobrados. O limite está em `motor.cobertura.minima` no `pom.xml`.

Os testes do motor são JUnit puro (sem Spring) e rodam em segundos:

```bash
./mvnw test -Dtest='br.com.codeacle.campzone.motor.**.*Test'
```

## CI

`.github/workflows/ci.yml` roda `./mvnw -B verify` com Temurin 21 em pull requests e em push na `main`.
