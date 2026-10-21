package br.com.codeacle.campzone.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import br.com.codeacle.campzone.common.ApiTesteController;
import br.com.codeacle.campzone.common.error.GlobalExceptionHandler;

@WebMvcTest(controllers = ApiTesteController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SecurityConfigWebTest {

    @Autowired
    private MockMvc mockMvc;

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
    void rotaPrivadaSemAutenticacaoDevolve401ComoProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/privado"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void postSemAutenticacaoNaoExigeCsrfEDevolve401() throws Exception {
        mockMvc.perform(post("/api/v1/privado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void rotaPrivadaAutenticadaFunciona() throws Exception {
        mockMvc.perform(get("/api/v1/privado"))
                .andExpect(status().isOk())
                .andExpect(content().string("segredo"));
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
}
