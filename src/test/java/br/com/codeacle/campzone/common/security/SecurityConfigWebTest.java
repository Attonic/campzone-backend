package br.com.codeacle.campzone.common.security;

import static br.com.codeacle.campzone.support.TokensDeTeste.admin;
import static br.com.codeacle.campzone.support.TokensDeTeste.organizador;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.codeacle.campzone.auth.api.AuthController;
import br.com.codeacle.campzone.auth.api.response.UsuarioResponse;
import br.com.codeacle.campzone.auth.domain.Papel;
import br.com.codeacle.campzone.auth.service.AuthService;
import br.com.codeacle.campzone.support.SegurancaWebTest;
import br.com.codeacle.campzone.support.TokensDeTeste;
import br.com.codeacle.testes.ApiTesteController;
import jakarta.servlet.DispatcherType;

@WebMvcTest(controllers = { ApiTesteController.class, AuthController.class })
@Import(ApiTesteController.class)
@SegurancaWebTest
class SecurityConfigWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void nomesDosPapeisDaSecurityConfigSaoOsDoEnum() {
        org.assertj.core.api.Assertions.assertThat(SecurityConfig.PAPEL_ADMIN).isEqualTo(Papel.ADMIN_PLATAFORMA.name());
        org.assertj.core.api.Assertions.assertThat(SecurityConfig.PAPEL_ORGANIZADOR)
                .isEqualTo(Papel.ORGANIZADOR.name());
    }

    @Test
    void rotaPublicaNaoExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/public/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void rotaPublicaInexistenteNaoDa401() throws Exception {
        mockMvc.perform(get("/api/v1/public/qualquer"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rotaPublicaIgnoraTokenInvalidoEnviado() throws Exception {
        mockMvc.perform(get("/api/v1/public/ping").header(HttpHeaders.AUTHORIZATION, bearer("lixo")))
                .andExpect(status().isOk());
    }

    @Test
    void rotaPrivadaSemAutenticacaoDevolve401ComoProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/privado"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Autenticação necessária."));
    }

    @Test
    void postSemAutenticacaoNaoExigeCsrfEDevolve401() throws Exception {
        mockMvc.perform(post("/api/v1/privado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dispatchDeErroNaoViraUnauthorized() throws Exception {
        // Simula o dispatch interno do container para /error (ex.: erro numa rota pública).
        mockMvc.perform(get("/error").with(request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            return request;
        }))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    // --- Papéis (ADR 0002) ---

    @Test
    void organizadorAcessaRotaDeOrganizador() throws Exception {
        mockMvc.perform(get("/api/v1/privado").with(organizador(1, 10)))
                .andExpect(status().isOk())
                .andExpect(content().string("segredo"));
    }

    @Test
    void organizadorRecebe403EmRotaDeAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/teste").with(organizador(1, 10)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminAcessaRotaDeAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/teste").with(admin(2)))
                .andExpect(status().isOk());
    }

    @Test
    void adminRecebe403EmRotaDeOrganizador() throws Exception {
        mockMvc.perform(get("/api/v1/privado").with(admin(2)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void adminSemOrganizacaoRecebe403QuandoARotaPedeOrganizacaoAtual() throws Exception {
        // Defesa em profundidade: mesmo que uma rota de organizador fosse liberada ao admin, sem claim org é 403.
        mockMvc.perform(get("/api/v1/teste/organizacao-atual").with(admin(2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizacaoAtualPrimitivaTambemVemDoTokenEIgnoraAQuery() throws Exception {
        mockMvc.perform(get("/api/v1/teste/organizacao-atual-primitivo?organizacaoId=99").with(organizador(1, 10)))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    void organizacaoAtualEmTipoNaoSuportadoFalhaEmVezDeLerAQuery() throws Exception {
        mockMvc.perform(get("/api/v1/teste/organizacao-atual-texto?organizacaoId=99").with(organizador(1, 10)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("99"))));
        mockMvc.perform(get("/api/v1/teste/organizacao-atual-inteiro?organizacaoId=99").with(organizador(1, 10)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "ORGANIZADOR")
    void autenticacaoQueNaoEJwtNaoResolveOrganizacao() throws Exception {
        mockMvc.perform(get("/api/v1/teste/organizacao-atual"))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizacaoAtualVemDoClaimDoToken() throws Exception {
        mockMvc.perform(get("/api/v1/teste/organizacao-atual").with(organizador(1, 10)))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    void rotaForaDoContratoENegadaMesmoAutenticado() throws Exception {
        mockMvc.perform(get("/actuator/env").with(organizador(1, 10)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ambosOsPapeisAcessamAuthMe() throws Exception {
        when(authService.buscarUsuario(anyLong())).thenReturn(
                new UsuarioResponse(1L, "Fulano", "f@x.com", Papel.ORGANIZADOR, 10L, "Org"));

        mockMvc.perform(get("/api/v1/auth/me").with(organizador(1, 10))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me").with(admin(2))).andExpect(status().isOk());
    }

    // --- Tokens reais passando pelo decoder HMAC ---

    @Test
    void tokenValidoAutenticaEAutorizaPeloPapel() throws Exception {
        String organizadorToken = TokensDeTeste.tokenValido("ORGANIZADOR", 10L);
        String adminToken = TokensDeTeste.tokenValido("ADMIN_PLATAFORMA", null);

        mockMvc.perform(get("/api/v1/teste/organizacao-atual").header(HttpHeaders.AUTHORIZATION,
                bearer(organizadorToken))).andExpect(status().isOk()).andExpect(content().string("10"));
        mockMvc.perform(get("/api/v1/privado").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/teste").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/teste").header(HttpHeaders.AUTHORIZATION, bearer(organizadorToken)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/privado", "/api/v1/auth/me" })
    void tokenMalformadoDevolve401(String rota) throws Exception {
        mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer("isto.nao.e-um-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Token inválido ou expirado."))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\""));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/privado", "/api/v1/auth/me" })
    void tokenAssinadoComOutroSegredoDevolve401(String rota) throws Exception {
        mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION,
                bearer(TokensDeTeste.tokenAssinadoComOutroSegredo())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/privado", "/api/v1/auth/me" })
    void tokenExpiradoDevolve401(String rota) throws Exception {
        mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer(TokensDeTeste.tokenExpirado())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void authMeSemTokenDevolve401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void tokenDeAlgoritmoNoneEhRejeitado() throws Exception {
        // JWT sem assinatura (alg=none): header {"alg":"none"} e payload de organizador.
        String semAssinatura = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes()) + "."
                + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                        ("{\"sub\":\"1\",\"papel\":\"ORGANIZADOR\",\"org\":1,\"exp\":" + (System.currentTimeMillis() / 1000 + 3600)
                                + "}").getBytes())
                + ".";

        mockMvc.perform(get("/api/v1/privado").header(HttpHeaders.AUTHORIZATION, bearer(semAssinatura)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void firewallBarraRequisicaoComPontoEVirgulaEmProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/public/a;b"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }
}
