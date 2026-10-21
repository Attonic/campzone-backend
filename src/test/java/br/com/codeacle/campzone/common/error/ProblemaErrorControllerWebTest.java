package br.com.codeacle.campzone.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.codeacle.campzone.support.SegurancaWebTest;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;

@WebMvcTest(controllers = ProblemaErrorController.class)
@SegurancaWebTest
class ProblemaErrorControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions erro(Integer codigo) throws Exception {
        return mockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, "detalhe interno que não pode vazar")
                .with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    if (codigo != null) {
                        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, codigo);
                    }
                    return request;
                }));
    }

    @Test
    void errosDoContainerSaemEmProblemDetailSemVazarMensagem() throws Exception {
        erro(400)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Bad Request"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("detalhe interno"))));
    }

    @Test
    void erro5xxTemDetalheGenerico() throws Exception {
        erro(503)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Erro interno inesperado."));
    }

    @Test
    void semStatusNoRequestViraErroInterno() throws Exception {
        erro(null)
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
