package br.com.codeacle.campzone.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import br.com.codeacle.campzone.support.SegurancaWebTest;
import br.com.codeacle.testes.ApiTesteController;
import br.com.codeacle.testes.ApiTesteValidacaoController;

@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest({ ApiTesteController.class, ApiTesteValidacaoController.class })
@Import({ ApiTesteController.class, ApiTesteValidacaoController.class })
@SegurancaWebTest
class GlobalExceptionHandlerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validacaoDevolveProblemDetailComCamposInvalidos() throws Exception {
        mockMvc.perform(post("/api/v1/public/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.erros[0].campo").value("nome"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("nome é obrigatório"));
    }

    @Test
    void erroInesperadoDevolve500SemVazarDetalhes() throws Exception {
        mockMvc.perform(get("/api/v1/public/explode"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Erro interno inesperado."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("detalhe interno"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("IllegalStateException"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void invarianteDeDominioDevolve400ComAMensagemDoDominio() throws Exception {
        mockMvc.perform(get("/api/v1/public/invariante"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dado inválido"))
                .andExpect(jsonPath("$.detail").value("Nome é obrigatório."));
    }

    @Test
    void rotaInexistenteDevolve404ComoProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/public/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void accessDeniedAnonimoVira401EmVezDe500() throws Exception {
        mockMvc.perform(get("/api/v1/public/acesso-negado"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser
    void accessDeniedAutenticadoVira403EmVezDe500() throws Exception {
        mockMvc.perform(get("/api/v1/public/acesso-negado"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void authenticationExceptionVira401EmVezDe500() throws Exception {
        mockMvc.perform(get("/api/v1/public/nao-autenticado"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void excecaoComResponseStatusMantemOStatusDeclarado() throws Exception {
        mockMvc.perform(get("/api/v1/public/conflito"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Recurso em conflito"));
    }

    @Test
    void responseStatus5xxMantemOStatusELogaOErro(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/v1/public/indisponivel"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        org.assertj.core.api.Assertions.assertThat(output.getAll()).contains("falha-5xx-anotada");
    }

    @Test
    void parametroValidoPassa() throws Exception {
        mockMvc.perform(get("/api/v1/public/teste-validacao/parametros?valor=5&texto=abc"))
                .andExpect(status().isOk());
    }

    @Test
    void parametroInvalidoDevolve400ComOsCamposNoMesmoFormatoDaValidacaoDeBody() throws Exception {
        mockMvc.perform(get("/api/v1/public/teste-validacao/parametros?valor=0&texto=abcd"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.erros.length()").value(2))
                .andExpect(jsonPath("$.erros[0].campo").value("texto"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("máximo de 3 caracteres"))
                .andExpect(jsonPath("$.erros[1].campo").value("valor"))
                .andExpect(jsonPath("$.erros[1].mensagem").value("valor mínimo é 1"));
    }

    @Test
    void violacaoDeBeanNaoViraErroDoCliente() throws Exception {
        mockMvc.perform(get("/api/v1/public/teste-validacao/bean"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Erro interno inesperado."));
    }
}
