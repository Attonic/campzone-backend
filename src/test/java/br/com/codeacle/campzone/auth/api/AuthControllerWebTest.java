package br.com.codeacle.campzone.auth.api;

import static br.com.codeacle.campzone.support.TokensDeTeste.admin;
import static br.com.codeacle.campzone.support.TokensDeTeste.organizador;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.codeacle.campzone.auth.api.request.CadastrarOrganizacaoRequest;
import br.com.codeacle.campzone.auth.api.response.AutenticacaoResponse;
import br.com.codeacle.campzone.auth.api.response.UsuarioResponse;
import br.com.codeacle.campzone.auth.domain.Papel;
import br.com.codeacle.campzone.auth.exception.CredenciaisInvalidasException;
import br.com.codeacle.campzone.auth.exception.EmailJaCadastradoException;
import br.com.codeacle.campzone.auth.service.AuthService;
import br.com.codeacle.campzone.support.SegurancaWebTest;

@WebMvcTest(controllers = AuthController.class)
@SegurancaWebTest
class AuthControllerWebTest {

    private static final UsuarioResponse USUARIO = new UsuarioResponse(9L, "Ana", "ana@x.com", Papel.ORGANIZADOR, 5L,
            "Minha Org");
    private static final AutenticacaoResponse AUTENTICACAO = new AutenticacaoResponse("jwt-emitido", "Bearer",
            Instant.parse("2026-10-21T20:00:00Z"), USUARIO);

    private static final String CADASTRO_VALIDO = """
            {"nomeOrganizacao":"Minha Org","nome":"Ana","email":"ana@x.com","senha":"senha-forte-1"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void cadastroDevolve201ComOContratoDaAutenticacao() throws Exception {
        when(authService.cadastrar(any())).thenReturn(AUTENTICACAO);

        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(CADASTRO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-emitido"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").value("2026-10-21T20:00:00Z"))
                .andExpect(jsonPath("$.usuario.id").value(9))
                .andExpect(jsonPath("$.usuario.nome").value("Ana"))
                .andExpect(jsonPath("$.usuario.email").value("ana@x.com"))
                .andExpect(jsonPath("$.usuario.papel").value("ORGANIZADOR"))
                .andExpect(jsonPath("$.usuario.organizacaoId").value(5))
                .andExpect(jsonPath("$.usuario.organizacaoNome").value("Minha Org"));

        ArgumentCaptor<CadastrarOrganizacaoRequest> request = ArgumentCaptor
                .forClass(CadastrarOrganizacaoRequest.class);
        verify(authService).cadastrar(request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue())
                .isEqualTo(new CadastrarOrganizacaoRequest("Minha Org", "Ana", "ana@x.com", "senha-forte-1"));
    }

    @Test
    void cadastroInvalidoDevolve400ComErrosPorCampo() throws Exception {
        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nomeOrganizacao\":\"A\",\"nome\":\"\",\"email\":\"nao-e-email\",\"senha\":\"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros[?(@.campo=='nomeOrganizacao')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='senha')]").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void cadastroSemCamposDevolve400() throws Exception {
        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }

    @Test
    void cadastroRejeitaSenhaMaiorQue72() throws Exception {
        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nomeOrganizacao\":\"Org\",\"nome\":\"Ana\",\"email\":\"a@x.com\",\"senha\":\""
                        + "x".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("senha"));
    }

    @Test
    void cadastroConta72BytesENaoCaracteresNaSenha() throws Exception {
        // 40 "ç" = 40 caracteres mas 80 bytes em UTF-8: passaria no @Size(max = 72) e estouraria o BCrypt.
        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nomeOrganizacao\":\"Org\",\"nome\":\"Ana\",\"email\":\"a@x.com\",\"senha\":\""
                        + "ç".repeat(40) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros[?(@.campo=='senha')]").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void cadastroAceitaSenhaDe36CedilhasEQuePesam72Bytes() throws Exception {
        when(authService.cadastrar(any())).thenReturn(AUTENTICACAO);

        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nomeOrganizacao\":\"Org\",\"nome\":\"Ana\",\"email\":\"a@x.com\",\"senha\":\""
                        + "ç".repeat(36) + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void loginComSenhaLongaNaoTemRegraDeTamanhoNaBorda() throws Exception {
        when(authService.login(any())).thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/api/v1/public/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@x.com\",\"senha\":\"" + "ç".repeat(40) + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void cadastroComEmailDuplicadoDevolve409ComDetalheClaro() throws Exception {
        when(authService.cadastrar(any())).thenThrow(new EmailJaCadastradoException());

        mockMvc.perform(post("/api/v1/public/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(CADASTRO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Já existe um usuário cadastrado com este e-mail."));
    }

    @Test
    void loginDevolve200ComAAutenticacao() throws Exception {
        when(authService.login(any())).thenReturn(AUTENTICACAO);

        mockMvc.perform(post("/api/v1/public/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@x.com\",\"senha\":\"senha-forte-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-emitido"))
                .andExpect(jsonPath("$.usuario.email").value("ana@x.com"));
    }

    @Test
    void loginSemCamposDevolve400() throws Exception {
        mockMvc.perform(post("/api/v1/public/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='senha')]").exists());
    }

    @Test
    void loginRecusadoDevolve401ComDetalheUnico() throws Exception {
        when(authService.login(any())).thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/api/v1/public/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@x.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void meDevolveOUsuarioDoTokenIgnorandoQualquerIdNaRequisicao() throws Exception {
        when(authService.buscarUsuario(9L)).thenReturn(USUARIO);

        mockMvc.perform(get("/api/v1/auth/me?id=1").header("X-Usuario-Id", "1").with(organizador(9, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.papel").value("ORGANIZADOR"))
                .andExpect(jsonPath("$.organizacaoId").value(5));
        verify(authService).buscarUsuario(9L);
    }

    @Test
    void meDoAdminDevolveOrganizacaoNula() throws Exception {
        when(authService.buscarUsuario(1L)).thenReturn(
                new UsuarioResponse(1L, "Administrador", "admin@x.com", Papel.ADMIN_PLATAFORMA, null, null));

        mockMvc.perform(get("/api/v1/auth/me").with(admin(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN_PLATAFORMA"))
                .andExpect(jsonPath("$.organizacaoId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.organizacaoNome").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void meSemTokenDevolve401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    @Test
    void meDeUsuarioQueNaoExisteMaisDevolve401() throws Exception {
        when(authService.buscarUsuario(9L))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("sumiu"));

        mockMvc.perform(get("/api/v1/auth/me").with(organizador(9, 5)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
