package br.com.codeacle.campzone.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.com.codeacle.campzone.auth.seed.AdminSeeder;
import br.com.codeacle.campzone.auth.seed.DevDadosSeeder;
import br.com.codeacle.campzone.support.IntegracaoTest;
import br.com.codeacle.campzone.support.TokensDeTeste;
import com.jayway.jsonpath.JsonPath;

/** Fluxos de ponta a ponta: cadastro, login, token, papéis, isolamento base e seeds, contra o PostgreSQL real. */
@IntegracaoTest
class AuthIntegracaoTest {

    private static final String SENHA = "senha-forte-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AdminSeeder adminSeeder;

    @Autowired
    private DevDadosSeeder devDadosSeeder;

    private static String emailUnico() {
        return "user-" + UUID.randomUUID() + "@teste.com";
    }

    private static String json(String nomeOrg, String nome, String email, String senha) {
        return "{\"nomeOrganizacao\":\"" + nomeOrg + "\",\"nome\":\"" + nome + "\",\"email\":\"" + email
                + "\",\"senha\":\"" + senha + "\"}";
    }

    private MvcResult cadastrar(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json("Org " + email, "Fulano", email, SENHA)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/v1/public/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")).andReturn();
    }

    private static String token(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }

    private static Number organizacaoId(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.usuario.organizacaoId");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // --- Cadastro ---

    @Test
    void cadastroCriaOrganizacaoNoPlanoGratisEOrganizadorEOTokenFuncionaEmAuthMe() throws Exception {
        String email = emailUnico();

        MvcResult cadastro = mockMvc.perform(post("/api/v1/public/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(json("Minha Liga", "Ana Souza", email, SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.usuario.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andExpect(jsonPath("$.usuario.papel").value("ORGANIZADOR"))
                .andExpect(jsonPath("$.usuario.organizacaoNome").value("Minha Liga"))
                .andReturn();

        String plano = jdbc.queryForObject("""
                select p.codigo from organizacao o join plano p on p.id = o.plano_id where o.id = ?""",
                String.class, organizacaoId(cadastro).longValue());
        assertThat(plano).isEqualTo("GRATIS");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token(cadastro))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.papel").value("ORGANIZADOR"))
                .andExpect(jsonPath("$.organizacaoId").value(organizacaoId(cadastro).longValue()))
                .andExpect(jsonPath("$.organizacaoNome").value("Minha Liga"));
    }

    @Test
    void cadastroComEmailDuplicadoDevolve409MesmoComMaiusculasEEspacos() throws Exception {
        String email = emailUnico();
        cadastrar(email);
        long organizacoesAntes = jdbc.queryForObject("select count(*) from organizacao", Long.class);

        for (String variacao : new String[] { email, email.toUpperCase(), "  " + email + "  ",
                email.replace("user-", "USER-") }) {
            mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                    .content(json("Outra Org", "Beto", variacao, SENHA)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Já existe um usuário cadastrado com este e-mail."));
        }

        assertThat(jdbc.queryForObject("select count(*) from organizacao", Long.class))
                .as("cadastro recusado não deixa organização órfã").isEqualTo(organizacoesAntes);
    }

    @Test
    void cadastroInvalidoDevolve400ComErros() throws Exception {
        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json("A", "", "nao-e-email", "curta")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros[?(@.campo=='nomeOrganizacao')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='senha')]").exists());
    }

    @Test
    void cadastroComNomeSoDeEspacosViraDeInvarianteDeDominio400() throws Exception {
        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json("Org Valida", "    ", emailUnico(), SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // --- Login ---

    @Test
    void loginComCredenciaisCorretasDevolveTokenAceitoEmAuthMe() throws Exception {
        String email = emailUnico();
        cadastrar(email);

        MvcResult resultado = login(email.toUpperCase(), SENHA);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token(resultado))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void senhaErradaEEmailInexistenteDevolvemOMesmo401() throws Exception {
        String email = emailUnico();
        cadastrar(email);

        MvcResult senhaErrada = login(email, "senha-errada-123");
        MvcResult emailInexistente = login(emailUnico(), SENHA);
        MvcResult emailMalformado = login("nao-e-um-email", SENHA);

        for (MvcResult resultado : new MvcResult[] { senhaErrada, emailInexistente, emailMalformado }) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(resultado.getResponse().getContentType()).startsWith("application/problem+json");
            assertThat((String) JsonPath.read(resultado.getResponse().getContentAsString(), "$.detail"))
                    .isEqualTo("E-mail ou senha inválidos.");
        }
        assertThat(emailInexistente.getResponse().getContentAsString())
                .isEqualTo(senhaErrada.getResponse().getContentAsString());
    }

    @Test
    void loginSemCamposDevolve400() throws Exception {
        mockMvc.perform(post("/api/v1/public/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='senha')]").exists());
    }

    @Test
    void loginIgnoraTokenInvalidoEnviadoNoHeader() throws Exception {
        String email = emailUnico();
        cadastrar(email);

        mockMvc.perform(post("/api/v1/public/auth/login").header(HttpHeaders.AUTHORIZATION, bearer("token-velho"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void senhaFicaGuardadaComoHashBcrypt() throws Exception {
        String email = emailUnico();
        cadastrar(email);

        String hash = jdbc.queryForObject("select senha_hash from usuario where email = ?", String.class, email);

        assertThat(hash).startsWith("{bcrypt}$2").doesNotContain(SENHA);
    }

    @Test
    void emailGuardadoNormalizado() throws Exception {
        String email = emailUnico();
        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json("Org", "Ana", "  " + email.toUpperCase() + " ", SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.email").value(email));

        assertThat(jdbc.queryForObject("select count(*) from usuario where email = ?", Long.class, email))
                .isEqualTo(1);
    }

    // --- Token ---

    @Test
    void tokensInvalidosDao401EmAuthMe() throws Exception {
        String[] ruins = { "isto.nao.e-jwt", TokensDeTeste.tokenAssinadoComOutroSegredo(),
                TokensDeTeste.tokenExpirado() };

        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        for (String ruim : ruins) {
            mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(ruim)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    void tokenValidoDeUsuarioInexistenteDevolve401() throws Exception {
        // Assinado com o segredo da aplicação (dev) mas com um id que não existe.
        String segredoDev = "dev-nao-e-segredo-campzone-jwt-hmac-sha256-0123456789";
        java.time.Instant agora = java.time.Instant.now();
        String token = TokensDeTeste.token(segredoDev, agora, agora.plusSeconds(600), 999_999_999L, "ORGANIZADOR",
                1L);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    // --- Papéis ---

    @Test
    void organizadorRecebe403EmAdminEAdminRecebe403EmRotaDeOrganizador() throws Exception {
        String organizador = token(login("organizador@campzone.dev", "organizador123"));
        String admin = token(login("admin@campzone.dev", "admin12345"));

        mockMvc.perform(get("/api/v1/admin/teste").header(HttpHeaders.AUTHORIZATION, bearer(organizador)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/v1/admin/teste").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/privado").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/privado").header(HttpHeaders.AUTHORIZATION, bearer(organizador)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(organizador)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenDoAdminNaoCarregaOrganizacao() throws Exception {
        MvcResult admin = login("admin@campzone.dev", "admin12345");

        String payload = new String(java.util.Base64.getUrlDecoder().decode(token(admin).split("\\.")[1]));

        assertThat(payload).doesNotContain("\"org\"").contains("\"papel\":\"ADMIN_PLATAFORMA\"");
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
                .andExpect(jsonPath("$.papel").value("ADMIN_PLATAFORMA"))
                .andExpect(jsonPath("$.organizacaoId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.organizacaoNome").value(org.hamcrest.Matchers.nullValue()));
    }

    // --- Isolamento base ---

    @Test
    void organizacaoAtualVemSoDoTokenEIgnoraBodyQueryEHeader() throws Exception {
        MvcResult orgA = cadastrar(emailUnico());
        MvcResult orgB = cadastrar(emailUnico());
        long idA = organizacaoId(orgA).longValue();
        long idB = organizacaoId(orgB).longValue();
        assertThat(idA).isNotEqualTo(idB);

        mockMvc.perform(post("/api/v1/teste/organizacao-atual?organizacaoId=" + idB + "&org=" + idB)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(orgA)))
                .header("X-Organizacao-Id", idB)
                .header("organizacaoId", idB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizacaoId\":" + idB + ",\"org\":" + idB + "}"))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(idA)));

        mockMvc.perform(get("/api/v1/teste/organizacao-atual?organizacaoId=" + idB)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(orgA)))
                .header("X-Organizacao-Id", idB))
                .andExpect(content().string(String.valueOf(idA)));
        mockMvc.perform(get("/api/v1/teste/organizacao-atual")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(orgB))))
                .andExpect(content().string(String.valueOf(idB)));
    }

    @Test
    void adminRecebe403QuandoARotaDependeDeOrganizacao() throws Exception {
        String admin = token(login("admin@campzone.dev", "admin12345"));

        mockMvc.perform(get("/api/v1/teste/organizacao-atual").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isForbidden());
    }

    // --- Erros antes do DispatcherServlet ---

    @Test
    void firewallBarraRequisicaoComPontoEVirgulaEmProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/public/a;b"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- Seeds ---

    @Test
    void contasDeDevFuncionam() throws Exception {
        MvcResult organizador = login("organizador@campzone.dev", "organizador123");
        MvcResult admin = login("admin@campzone.dev", "admin12345");

        assertThat(organizador.getResponse().getStatus()).isEqualTo(200);
        assertThat(admin.getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token(organizador))))
                .andExpect(jsonPath("$.papel").value("ORGANIZADOR"))
                .andExpect(jsonPath("$.organizacaoNome").value("Organização Demo"));
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
                .andExpect(jsonPath("$.papel").value("ADMIN_PLATAFORMA"))
                .andExpect(jsonPath("$.organizacaoId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void rodarOsSeedersDeNovoNaoDuplica() throws Exception {
        long usuariosAntes = jdbc.queryForObject("select count(*) from usuario", Long.class);
        long demosAntes = jdbc.queryForObject("select count(*) from organizacao where nome = 'Organização Demo'",
                Long.class);

        adminSeeder.run(new DefaultApplicationArguments());
        devDadosSeeder.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("select count(*) from usuario", Long.class)).isEqualTo(usuariosAntes);
        assertThat(jdbc.queryForObject("select count(*) from organizacao where nome = 'Organização Demo'",
                Long.class)).isEqualTo(demosAntes).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from usuario where email = 'admin@campzone.dev'",
                Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*) from usuario where email = 'admin@campzone.dev' and organizacao_id is null""",
                Long.class)).isEqualTo(1);
    }
}
