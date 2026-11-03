package br.com.codeacle.campzone.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.repository.UsuarioRepository;
import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.campeonato.domain.ConfigPontosCorridosEmbutida;
import br.com.codeacle.campzone.campeonato.domain.StatusCampeonato;
import br.com.codeacle.campzone.campeonato.repository.CampeonatoRepository;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.organizacao.repository.OrganizacaoRepository;
import br.com.codeacle.campzone.support.IntegracaoTest;
import jakarta.persistence.EntityManagerFactory;

/**
 * Admin da plataforma de ponta a ponta (JWT real, PostgreSQL real). O banco é compartilhado com os outros testes de
 * integração, então os números do resumo são conferidos por diferença e as listas por um prefixo único de nome.
 */
@IntegracaoTest
class AdminIntegracaoTest {

    private static final String SENHA = "senha-forte-1";
    private static final String RESUMO = "/api/v1/admin/resumo";
    private static final String ORGANIZACOES = "/api/v1/admin/organizacoes";
    private static final Instant BASE = Instant.parse("2026-03-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transacao;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private OrganizacaoRepository organizacaoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CampeonatoRepository campeonatoRepository;

    /** Prefixo único por teste: as organizações deste teste são as únicas que casam com ele. */
    private String prefixo;
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        prefixo = "Adm" + UUID.randomUUID().toString().substring(0, 8) + " ";
        tokenAdmin = token(login("admin@campzone.dev", "admin12345"));
    }

    // --- apoio ---

    private MvcResult login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/v1/public/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn();
    }

    private static String token(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Cadastra pela API (organização no plano GRATIS e o primeiro organizador). Devolve o JSON do cadastro. */
    private MvcResult cadastrar(String nomeOrganizacao) throws Exception {
        String email = "adm-" + UUID.randomUUID() + "@teste.com";
        return mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nomeOrganizacao\":\"" + nomeOrganizacao + "\",\"nome\":\"Fulano\",\"email\":\"" + email
                        + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isCreated()).andReturn();
    }

    private long organizacao(String nome, Instant criadoEm) throws Exception {
        MvcResult cadastro = cadastrar(prefixo + nome);
        long id = ((Number) JsonPath.read(cadastro.getResponse().getContentAsString(), "$.usuario.organizacaoId"))
                .longValue();
        jdbc.update("update organizacao set criado_em = ? where id = ?", Timestamp.from(criadoEm), id);
        return id;
    }

    private void outroOrganizador(long organizacaoId) {
        transacao.executeWithoutResult(s -> {
            Organizacao organizacao = organizacaoRepository.findById(organizacaoId).orElseThrow();
            usuarioRepository.saveAndFlush(Usuario.organizador("Segundo", "seg-" + UUID.randomUUID() + "@teste.com",
                    "hash", organizacao));
        });
    }

    private void campeonato(long organizacaoId, StatusCampeonato status) {
        transacao.executeWithoutResult(s -> {
            Organizacao organizacao = organizacaoRepository.findById(organizacaoId).orElseThrow();
            Campeonato campeonato = campeonatoRepository.save(Campeonato.pontosCorridos(organizacao, "Liga",
                    "Futebol society", "adm-" + UUID.randomUUID(), ConfigPontosCorridosEmbutida.padrao(1)));
            if (status != StatusCampeonato.RASCUNHO) {
                campeonato.iniciar();
            }
            if (status == StatusCampeonato.FINALIZADO) {
                campeonato.finalizar();
            }
        });
    }

    private String json(String rota) throws Exception {
        return mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /** Pares nome/valor de parâmetros de query (o MockMvc cuida da codificação). */
    private MockHttpServletRequestBuilder listagem(String... parametros) {
        MockHttpServletRequestBuilder requisicao = get(ORGANIZACOES).header(HttpHeaders.AUTHORIZATION,
                bearer(tokenAdmin));
        for (int i = 0; i < parametros.length; i += 2) {
            requisicao.param(parametros[i], parametros[i + 1]);
        }
        return requisicao;
    }

    private String listar(String... parametros) throws Exception {
        return mockMvc.perform(listagem(parametros)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
    }

    private long numero(String json, String caminho) {
        return ((Number) JsonPath.read(json, caminho)).longValue();
    }

    // --- segurança ---

    @ParameterizedTest
    @ValueSource(strings = { RESUMO, ORGANIZACOES })
    void semTokenDevolve401EOrganizadorDevolve403(String rota) throws Exception {
        MvcResult cadastro = cadastrar(prefixo + "Org");
        String tokenOrganizador = token(cadastro);

        mockMvc.perform(get(rota))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer(tokenOrganizador)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void adminNaoAcessaRotasDeOrganizador() throws Exception {
        mockMvc.perform(get("/api/v1/privado").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/teste/organizacao-atual").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden());
    }

    // --- resumo ---

    @Test
    void resumoSomaDuasOrganizacoesComCampeonatosEmTodosOsStatus() throws Exception {
        String antes = json(RESUMO);

        long orgA = organizacao("A", BASE);
        outroOrganizador(orgA);
        campeonato(orgA, StatusCampeonato.RASCUNHO);
        campeonato(orgA, StatusCampeonato.EM_ANDAMENTO);
        campeonato(orgA, StatusCampeonato.FINALIZADO);
        long orgB = organizacao("B", BASE.plusSeconds(60));
        campeonato(orgB, StatusCampeonato.FINALIZADO);
        campeonato(orgB, StatusCampeonato.FINALIZADO);

        String depois = json(RESUMO);

        assertThat(numero(depois, "$.totalOrganizacoes") - numero(antes, "$.totalOrganizacoes")).isEqualTo(2);
        // A: dois organizadores; B: um. O admin já existia e conta no total, mas não pertence a organização.
        assertThat(numero(depois, "$.totalUsuarios") - numero(antes, "$.totalUsuarios")).isEqualTo(3);
        assertThat(numero(depois, "$.totalCampeonatos") - numero(antes, "$.totalCampeonatos")).isEqualTo(5);
        // FINALIZADO não conta como ativo: só o RASCUNHO e o EM_ANDAMENTO de A.
        assertThat(numero(depois, "$.campeonatosAtivos") - numero(antes, "$.campeonatosAtivos")).isEqualTo(2);
    }

    @Test
    void resumoBateComAContagemDiretaNoBanco() throws Exception {
        long org = organizacao("A", BASE);
        campeonato(org, StatusCampeonato.RASCUNHO);
        campeonato(org, StatusCampeonato.FINALIZADO);

        String resumo = json(RESUMO);

        assertThat(numero(resumo, "$.totalOrganizacoes"))
                .isEqualTo(jdbc.queryForObject("select count(*) from organizacao", Long.class));
        assertThat(numero(resumo, "$.totalUsuarios"))
                .isEqualTo(jdbc.queryForObject("select count(*) from usuario", Long.class));
        assertThat(numero(resumo, "$.totalCampeonatos"))
                .isEqualTo(jdbc.queryForObject("select count(*) from campeonato", Long.class));
        assertThat(numero(resumo, "$.campeonatosAtivos")).isEqualTo(jdbc.queryForObject(
                "select count(*) from campeonato where status in ('RASCUNHO', 'EM_ANDAMENTO')", Long.class));
        assertThat(numero(resumo, "$.totalUsuarios")).isEqualTo(jdbc.queryForObject(
                "select count(*) from usuario where papel = 'ADMIN_PLATAFORMA'", Long.class)
                + jdbc.queryForObject("select count(*) from usuario where papel = 'ORGANIZADOR'", Long.class));
    }

    // --- listagem ---

    @Test
    void listagemTrazPlanoEContagensDeCadaOrganizacao() throws Exception {
        long orgA = organizacao("A", BASE);
        outroOrganizador(orgA);
        campeonato(orgA, StatusCampeonato.RASCUNHO);
        campeonato(orgA, StatusCampeonato.EM_ANDAMENTO);
        campeonato(orgA, StatusCampeonato.FINALIZADO);
        long orgB = organizacao("B", BASE.plusSeconds(60));
        campeonato(orgB, StatusCampeonato.FINALIZADO);

        mockMvc.perform(listagem("busca", prefixo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.itens.length()").value(2))
                // mais nova primeiro: B, depois A
                .andExpect(jsonPath("$.itens[0].id").value(orgB))
                .andExpect(jsonPath("$.itens[0].nome").value(prefixo + "B"))
                .andExpect(jsonPath("$.itens[0].planoCodigo").value("GRATIS"))
                .andExpect(jsonPath("$.itens[0].planoNome").value("Grátis"))
                .andExpect(jsonPath("$.itens[0].totalUsuarios").value(1))
                .andExpect(jsonPath("$.itens[0].totalCampeonatos").value(1))
                .andExpect(jsonPath("$.itens[0].campeonatosAtivos").value(0))
                .andExpect(jsonPath("$.itens[0].criadoEm").value("2026-03-01T10:01:00Z"))
                .andExpect(jsonPath("$.itens[1].id").value(orgA))
                .andExpect(jsonPath("$.itens[1].nome").value(prefixo + "A"))
                .andExpect(jsonPath("$.itens[1].totalUsuarios").value(2))
                .andExpect(jsonPath("$.itens[1].totalCampeonatos").value(3))
                .andExpect(jsonPath("$.itens[1].campeonatosAtivos").value(2))
                .andExpect(jsonPath("$.itens[1].criadoEm").value("2026-03-01T10:00:00Z"));
    }

    @Test
    void organizacaoSemCampeonatosNemOutrosUsuariosVemComZeros() throws Exception {
        organizacao("Vazia", BASE);

        String lista = listar("busca", prefixo);

        assertThat(numero(lista, "$.itens[0].totalUsuarios")).isEqualTo(1);
        assertThat(numero(lista, "$.itens[0].totalCampeonatos")).isZero();
        assertThat(numero(lista, "$.itens[0].campeonatosAtivos")).isZero();
    }

    @Test
    void ordenaPorCriacaoDecrescenteEDesempataPorIdDecrescente() throws Exception {
        long velha = organizacao("Velha", BASE);
        long empateA = organizacao("EmpateA", BASE.plusSeconds(60));
        long empateB = organizacao("EmpateB", BASE.plusSeconds(60));
        long nova = organizacao("Nova", BASE.plusSeconds(120));

        String lista = listar("busca", prefixo);

        assertThat(empateB).isGreaterThan(empateA);
        assertThat(numero(lista, "$.itens[0].id")).isEqualTo(nova);
        assertThat(numero(lista, "$.itens[1].id")).isEqualTo(empateB);
        assertThat(numero(lista, "$.itens[2].id")).isEqualTo(empateA);
        assertThat(numero(lista, "$.itens[3].id")).isEqualTo(velha);
    }

    @Test
    void paginaSemRepetirNemPerderEPaginaAlemDaUltimaVemVazia() throws Exception {
        for (int i = 0; i < 5; i++) {
            organizacao("Org" + i, BASE.plusSeconds(i));
        }

        String pagina0 = listar("busca", prefixo, "pagina", "0", "tamanho", "2");
        String pagina1 = listar("busca", prefixo, "pagina", "1", "tamanho", "2");
        String pagina2 = listar("busca", prefixo, "pagina", "2", "tamanho", "2");
        String pagina3 = listar("busca", prefixo, "pagina", "3", "tamanho", "2");
        String gigante = listar("busca", prefixo, "pagina", String.valueOf(Integer.MAX_VALUE), "tamanho", "100");

        assertThat(numero(pagina0, "$.totalItens")).isEqualTo(5);
        assertThat(numero(pagina0, "$.totalPaginas")).isEqualTo(3);
        assertThat(numero(pagina0, "$.tamanho")).isEqualTo(2);
        assertThat(JsonPath.<List<String>>read(pagina0, "$.itens[*].nome"))
                .containsExactly(prefixo + "Org4", prefixo + "Org3");
        assertThat(JsonPath.<List<String>>read(pagina1, "$.itens[*].nome"))
                .containsExactly(prefixo + "Org2", prefixo + "Org1");
        assertThat(JsonPath.<List<String>>read(pagina2, "$.itens[*].nome"))
                .containsExactly(prefixo + "Org0");
        assertThat(JsonPath.<List<String>>read(pagina3, "$.itens[*].nome")).isEmpty();
        assertThat(numero(pagina3, "$.pagina")).isEqualTo(3);
        assertThat(numero(pagina3, "$.totalItens")).isEqualTo(5);
        assertThat(JsonPath.<List<String>>read(gigante, "$.itens[*].nome")).isEmpty();
        assertThat(numero(gigante, "$.pagina")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void buscaNaoDiferenciaMaiusculasEFiltraPorNomeContendoOTexto() throws Exception {
        organizacao("Liga dos Amigos", BASE);
        organizacao("Copa Society", BASE.plusSeconds(1));
        organizacao("Clube de Xadrez", BASE.plusSeconds(2));

        String liga = listar("busca", prefixo.toUpperCase() + "LIGA DOS");
        String society = listar("busca", prefixo.toLowerCase() + "cOpA sOcIeTy");
        String ambas = listar("busca", prefixo, "tamanho", "1");
        String nenhuma = listar("busca", prefixo + "inexistente");

        assertThat(JsonPath.<List<String>>read(liga, "$.itens[*].nome"))
                .containsExactly(prefixo + "Liga dos Amigos");
        assertThat(JsonPath.<List<String>>read(society, "$.itens[*].nome"))
                .containsExactly(prefixo + "Copa Society");
        assertThat(numero(ambas, "$.totalItens")).isEqualTo(3);
        assertThat(numero(ambas, "$.totalPaginas")).isEqualTo(3);
        assertThat(numero(nenhuma, "$.totalItens")).isZero();
        assertThat(numero(nenhuma, "$.totalPaginas")).isZero();
        assertThat(JsonPath.<List<String>>read(nenhuma, "$.itens")).isEmpty();
    }

    @Test
    void caracteresCuringaDaBuscaSaoLiterais() throws Exception {
        organizacao("100% Futebol", BASE);
        organizacao("100 Futebol", BASE.plusSeconds(1));
        organizacao("Time_A", BASE.plusSeconds(2));
        organizacao("TimeXA", BASE.plusSeconds(3));

        String porcento = listar("busca", prefixo + "100%");
        String underline = listar("busca", prefixo + "time_a");
        String soCuringa = listar("busca", "%");

        assertThat(JsonPath.<List<String>>read(porcento, "$.itens[*].nome"))
                .containsExactly(prefixo + "100% Futebol");
        assertThat(JsonPath.<List<String>>read(underline, "$.itens[*].nome"))
                .containsExactly(prefixo + "Time_A");
        // "%" sozinho procura o símbolo, não "tudo": só casa nomes que o contêm.
        assertThat(JsonPath.<List<String>>read(soCuringa, "$.itens[*].nome"))
                .allSatisfy(nome -> assertThat(nome).contains("%"));
    }

    @Test
    void buscaVaziaOuSoComEspacosListaTudo() throws Exception {
        organizacao("A", BASE);

        String vazia = listar("busca", "", "tamanho", "100");
        String espacos = listar("busca", "  ", "tamanho", "100");

        assertThat(numero(vazia, "$.totalItens")).isEqualTo(jdbc.queryForObject("select count(*) from organizacao",
                Long.class));
        assertThat(numero(espacos, "$.totalItens")).isEqualTo(numero(vazia, "$.totalItens"));
    }

    // --- validação ---

    @ParameterizedTest
    @ValueSource(strings = { "pagina=-1", "tamanho=0", "tamanho=101", "pagina=abc", "tamanho=1.5",
            "pagina=99999999999" })
    void parametrosInvalidosDevolvem400ComoProblemDetail(String query) throws Exception {
        String[] parte = query.split("=");
        mockMvc.perform(listagem(parte[0], parte[1]))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void buscaDeMaisDe100CaracteresDevolve400ELimiteExatoPassa() throws Exception {
        mockMvc.perform(listagem("busca", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros[0].campo").value("busca"));
        mockMvc.perform(listagem("busca", "a".repeat(100)))
                .andExpect(status().isOk());
    }

    // --- sem N+1 ---

    @Test
    void quantidadeDeConsultasNaoCresceComONumeroDeOrganizacoes() throws Exception {
        Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estatisticas.setStatisticsEnabled(true);
        try {
            for (int i = 0; i < 2; i++) {
                long org = organizacao("Org" + i, BASE.plusSeconds(i));
                outroOrganizador(org);
                campeonato(org, StatusCampeonato.RASCUNHO);
                campeonato(org, StatusCampeonato.FINALIZADO);
            }
            ConsultasExecutadas comDuas = medir(estatisticas, prefixo);

            for (int i = 2; i < 12; i++) {
                long org = organizacao("Org" + i, BASE.plusSeconds(i));
                outroOrganizador(org);
                campeonato(org, StatusCampeonato.EM_ANDAMENTO);
                campeonato(org, StatusCampeonato.FINALIZADO);
                campeonato(org, StatusCampeonato.FINALIZADO);
            }
            ConsultasExecutadas comDoze = medir(estatisticas, prefixo);

            assertThat(comDoze.pagina()).isEqualTo(comDuas.pagina());
            assertThat(comDoze.resumo()).isEqualTo(comDuas.resumo());
            // Página: uma consulta de contagem e uma consulta com as contagens agregadas. Resumo: 4 contagens.
            assertThat(comDoze.pagina()).isLessThanOrEqualTo(2);
            assertThat(comDoze.resumo()).isLessThanOrEqualTo(4);
            // As entidades nunca são carregadas (projeção): nada de lazy loading em laço.
            assertThat(comDoze.entidadesCarregadas()).isZero();
        } finally {
            estatisticas.setStatisticsEnabled(false);
        }
    }

    private record ConsultasExecutadas(long pagina, long resumo, long entidadesCarregadas) {
    }

    private ConsultasExecutadas medir(Statistics estatisticas, String prefixo) throws Exception {
        estatisticas.clear();
        String pagina = listar("busca", prefixo, "tamanho", "100");
        long consultasDaPagina = estatisticas.getPrepareStatementCount();
        long entidades = estatisticas.getEntityLoadCount() + estatisticas.getEntityFetchCount()
                + estatisticas.getCollectionLoadCount();
        assertThat(JsonPath.<List<Object>>read(pagina, "$.itens")).isNotEmpty();

        estatisticas.clear();
        json(RESUMO);
        long consultasDoResumo = estatisticas.getPrepareStatementCount();
        return new ConsultasExecutadas(consultasDaPagina, consultasDoResumo, entidades);
    }
}
