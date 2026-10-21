# CampZone: Backend

Vale para tudo dentro de `backend/`. Regras comuns (contrato, DoD, git) estão no `CLAUDE.md` da raiz.

## Stack

Java 21, Spring Boot 4.x (estável mais recente; hoje 4.1.1), Maven (`./mvnw`), PostgreSQL, Spring Data JPA/Hibernate, Spring Security + JWT stateless, JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL). Monólito modular.

**Spring Boot 4 não é Boot 3.** Antes de escrever config ou dependências, consulte o Migration Guide oficial. Pontos conhecidos: starters modulares (`spring-boot-starter-webmvc`, `-data-jpa`, `-security`, `-validation`, e starters de teste `*-test`), Jackson 3 (pacote `tools.jackson`), Spring Framework 7. Não assuma APIs do Boot 3.

Check obrigatório: `cd backend && ./mvnw verify`.

## Arquitetura

Pacote base: `br.com.codeacle.campzone`.

Módulos (pacotes): `auth`, `organizacao`, `plano`, `campeonato`, `participante`, `partida`, `classificacao`, `motor`, `publico`, `common`.

Estrutura dentro de cada módulo (exemplo `campeonato`):

```
campeonato/
  api/
    CampeonatoController.java
    request/CriarCampeonatoRequest.java
    request/AtualizarCampeonatoRequest.java
    response/CampeonatoResponse.java
  mapper/CampeonatoMapper.java
  service/
    CampeonatoService.java            # interface
    impl/CampeonatoServiceImpl.java   # implementação
  repository/CampeonatoRepository.java
  domain/Campeonato.java
```

### Padrão Request / Response

- Todo payload de entrada é `XxxRequest` e todo de saída é `XxxResponse`, sempre `record`. Nomes em português com verbo no Request: `CriarCampeonatoRequest`, `LancarResultadoRequest`, `CampeonatoResponse`, `ClassificacaoResponse`.
- `Request` carrega Bean Validation (`@NotBlank`, `@Size`, `@Valid`...). Nunca recebe `organizacaoId`.
- `Response` só tem dados, sem lógica. **Entidade JPA nunca sai na API nem entra por ela.**
- Conversão entidade ↔ DTO só no `Mapper` do módulo (`@Component`, manual, sem MapStruct). Controller não conhece entidade.

### Padrão Service com implementação

- Todo service de negócio é uma **interface** (`CampeonatoService`) mais uma **implementação** (`CampeonatoServiceImpl`) em `service/impl`, anotada com `@Service`.
- O service recebe `Request` e devolve `Response`; o `organizacaoId` entra como parâmetro explícito, vindo do JWT. Ex.: `CampeonatoResponse criar(Long organizacaoId, CriarCampeonatoRequest request)`.
- Quem usa (controller, outros services) injeta **a interface**, nunca a `Impl`.
- Transação na `Impl` (`@Transactional`, leitura com `readOnly = true`).
- `LimitePlanoService` também segue o padrão (interface + `LimitePlanoServiceImpl`).
- Exceções: `motor` (Java puro, usa `FormatoCompeticao` + implementações) e `Repository` (interface Spring Data) não seguem o par Service/Impl.

### Regras gerais

- Controllers finos: só HTTP, validação (`@Valid`) e delegação ao service. Regra de negócio fica na `Impl` ou no `motor`.
- Injeção por construtor. Sem field injection.
- Erros: `ProblemDetail` via `@RestControllerAdvice`. Exceções de domínio próprias.
- Invariantes de domínio ficam no domínio, além da validação nas bordas.
- Sem N+1: `@EntityGraph`/`fetch join` ou projeções; revisar toda listagem.
- Sem `System.out`, use SLF4J. Sem segredos no código.

### Banco de dados

- PostgreSQL. **Sem migrations e sem Flyway** (decisão do Antonio). O schema é gerado pelo Hibernate a partir das entidades (`spring.jpa.hibernate.ddl-auto=update`).
- Constraints e índices nas anotações JPA (`@Table(uniqueConstraints, indexes)`, `@Column`).
- Dados iniciais (plano grátis) por um `ApplicationRunner` idempotente.
- Mudança de entidade que possa quebrar dados existentes (renomear/remover coluna, tipo incompatível): avisar o orquestrador antes de fazer.

### Multi-tenant (inegociável)

- Toda entidade de negócio tem `organizacao_id`.
- `organizacaoId` vem **do JWT**, nunca do body, query ou path.
- Todo acesso a dados filtra por organização. Buscar por id sempre como `findByIdAndOrganizacaoId`.
- Toda feature que expõe dados precisa de teste provando que a org A não lê nem altera dados da org B.
- A página pública lê só campeonatos marcados como públicos, por `slug`.

### Limites do plano

`LimitePlanoService` é o único lugar que valida `maxCampeonatosAtivos` e `maxParticipantes`. Violação lança `LimitePlanoExcedidoException` (HTTP 409 com ProblemDetail explicando o limite).

## Motor de competição (`motor`)

Núcleo do produto. **Java puro**: sem Spring, sem JPA, sem I/O. Entrada e saída em `record`s imutáveis (próprios do motor, sem sufixo Request/Response). Tudo determinístico. Os mappers do módulo traduzem entre o motor e os DTOs da API.

Interface central `FormatoCompeticao` (Strategy), com duas implementações: `PontosCorridosFormato` e `MataMataFormato`. Cada uma recebe sua própria config tipada (`ConfigPontosCorridos`, `ConfigMataMata`) e é responsável por: (1) gerar a estrutura inicial de partidas, (2) calcular o estado a partir dos resultados (classificação ou avanço de fase). Refine a assinatura com um ADR curto antes de implementar.

### Pontos corridos

Config: turnos (único, ida e volta, N turnos); pontos por vitória/empate/derrota (padrão 3/1/0); permite empate (sim/não); lista **ordenada** de critérios de desempate (pontos, vitórias, saldo, pró, confronto direto, sorteio).

Regras:
- Geração round-robin (método do círculo). Número ímpar → folga (bye) por rodada, sem partida fantasma.
- Ida e volta inverte o mando. Rodadas balanceadas (evitar mesmo time mandante em sequência quando possível).
- Desempate aplicado na ordem configurada. Confronto direto considera só os times empatados entre si (mini-tabela).
- Se `permiteEmpate = false`, resultado empatado é rejeitado.

### Mata-mata (eliminatória simples)

Config: confronto (jogo único, ida e volta, série melhor de N); desempate (gols fora, prorrogação, pênaltis, sorteio); seeding (aleatório, manual, cabeças de chave); disputa de 3º lugar (sim/não).

Regras:
- Chave de tamanho potência de 2. Quantidade que não é potência de 2 → byes, distribuídos para os melhores cabeças de chave.
- Seeding clássico: 1 vs último, 2 vs penúltimo, de forma que 1 e 2 só se encontrem na final.
- Sorteio com **seed persistida** (reprodutível e auditável).
- Avanço de fase automático ao fechar todos os confrontos da fase.
- Resultado que não define vencedor (empate sem critério de desempate resolvido) é rejeitado.

### Testes do motor

Cobertura mínima de 90% no pacote `motor` (JaCoCo). Casos obrigatórios:
- Pontos corridos: 2, 3, 4, 5, 8, 9 participantes; turno único e ida e volta; cada critério de desempate isolado e combinado.
- Mata-mata: 2, 3, 4, 5, 8, 11, 16 participantes; com e sem 3º lugar; jogo único, ida e volta, série; cada tipo de desempate.
- Propriedades: nenhum confronto repetido (turno único), todos enfrentam todos, mandos equilibrados, mesma seed gera mesmo sorteio.

## Testes (demais camadas)

- `Impl` de service: teste unitário com Mockito nos repositories.
- Controller: teste de web layer mockando a **interface** do service.
- Repositories e isolamento multi-tenant: Testcontainers com PostgreSQL.
