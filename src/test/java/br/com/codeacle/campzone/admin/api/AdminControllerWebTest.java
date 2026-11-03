package br.com.codeacle.campzone.admin.api;

import static br.com.codeacle.campzone.support.TokensDeTeste.admin;
import static br.com.codeacle.campzone.support.TokensDeTeste.organizador;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.codeacle.campzone.admin.api.response.OrganizacaoAdminResponse;
import br.com.codeacle.campzone.admin.api.response.ResumoPlataformaResponse;
import br.com.codeacle.campzone.admin.service.AdminPlataformaService;
import br.com.codeacle.campzone.common.api.response.PaginaResponse;
import br.com.codeacle.campzone.support.SegurancaWebTest;

@WebMvcTest(controllers = AdminController.class)
@SegurancaWebTest
class AdminControllerWebTest {

    private static final String RESUMO = "/api/v1/admin/resumo";
    private static final String ORGANIZACOES = "/api/v1/admin/organizacoes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPlataformaService service;

    // --- segurança ---

    @ParameterizedTest
    @ValueSource(strings = { RESUMO, ORGANIZACOES })
    void semTokenDevolve401ComoProblemDetail(String rota) throws Exception {
        mockMvc.perform(get(rota))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = { RESUMO, ORGANIZACOES })
    void organizadorDevolve403ComoProblemDetail(String rota) throws Exception {
        mockMvc.perform(get(rota).with(organizador(1, 10)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(service);
    }

    // --- contrato ---

    @Test
    void resumoDevolveOsQuatroNumerosDoContrato() throws Exception {
        when(service.resumo()).thenReturn(new ResumoPlataformaResponse(3, 5, 11, 7));

        mockMvc.perform(get(RESUMO).with(admin(2)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"totalOrganizacoes":3,"totalUsuarios":5,"totalCampeonatos":11,"campeonatosAtivos":7}""",
                        true));
    }

    @Test
    void organizacoesDevolveAPaginaDoContrato() throws Exception {
        when(service.listarOrganizacoes(1, 5, "liga")).thenReturn(new PaginaResponse<>(
                List.of(new OrganizacaoAdminResponse(9L, "Liga dos Amigos", "GRATIS", "Grátis", 2, 5, 3,
                        Instant.parse("2026-10-21T20:00:00Z"))),
                1, 5, 6, 2));

        mockMvc.perform(get(ORGANIZACOES + "?pagina=1&tamanho=5&busca=liga").with(admin(2)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"itens":[{"id":9,"nome":"Liga dos Amigos","planoCodigo":"GRATIS","planoNome":"Grátis",
                        "totalUsuarios":2,"totalCampeonatos":5,"campeonatosAtivos":3,
                        "criadoEm":"2026-10-21T20:00:00Z"}],
                        "pagina":1,"tamanho":5,"totalItens":6,"totalPaginas":2}""", true));
    }

    @Test
    void semParametrosUsaPagina0Tamanho20SemBusca() throws Exception {
        when(service.listarOrganizacoes(anyInt(), anyInt(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(ORGANIZACOES).with(admin(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20));

        verify(service).listarOrganizacoes(0, 20, null);
    }

    @Test
    void parametroComValoresNosLimitesEAceito() throws Exception {
        when(service.listarOrganizacoes(anyInt(), anyInt(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 1, 0, 0));
        String buscaDe100 = "a".repeat(100);

        mockMvc.perform(get(ORGANIZACOES + "?pagina=0&tamanho=1").with(admin(2))).andExpect(status().isOk());
        mockMvc.perform(get(ORGANIZACOES + "?pagina=999&tamanho=100").with(admin(2))).andExpect(status().isOk());
        mockMvc.perform(get(ORGANIZACOES + "?busca=" + buscaDe100).with(admin(2))).andExpect(status().isOk());

        verify(service).listarOrganizacoes(0, 1, null);
        verify(service).listarOrganizacoes(999, 100, null);
        verify(service).listarOrganizacoes(0, 20, buscaDe100);
    }

    @Test
    void buscaVaziaChegaAoServiceEEleDecideOQueFazer() throws Exception {
        when(service.listarOrganizacoes(anyInt(), anyInt(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(ORGANIZACOES + "?busca=").with(admin(2))).andExpect(status().isOk());

        verify(service).listarOrganizacoes(0, 20, "");
    }

    @Test
    void organizacaoIdNaQueryEIgnorado() throws Exception {
        when(service.listarOrganizacoes(anyInt(), anyInt(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(ORGANIZACOES + "?organizacaoId=5").with(admin(2))).andExpect(status().isOk());

        verify(service).listarOrganizacoes(0, 20, null);
    }

    // --- validação dos parâmetros ---

    @ParameterizedTest
    @ValueSource(strings = {
            "pagina=-1",
            "tamanho=0",
            "tamanho=-5",
            "tamanho=101",
            "pagina=abc",
            "tamanho=abc",
            "pagina=1.5",
            "pagina=99999999999",
            "tamanho=99999999999" })
    void parametroForaDoLimiteDevolve400ComoProblemDetail(String query) throws Exception {
        mockMvc.perform(get(ORGANIZACOES + "?" + query).with(admin(2)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @Test
    void buscaComMaisDe100CaracteresDevolve400() throws Exception {
        mockMvc.perform(get(ORGANIZACOES + "?busca=" + "a".repeat(101)).with(admin(2)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros[0].campo").value("busca"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("A busca deve ter no máximo 100 caracteres."));

        verifyNoInteractions(service);
    }

    @Test
    void erroDeValidacaoListaOsCamposComProblema() throws Exception {
        mockMvc.perform(get(ORGANIZACOES + "?pagina=-1&tamanho=101").with(admin(2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.erros.length()").value(2))
                .andExpect(jsonPath("$.erros[0].campo").value("pagina"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("A página deve ser maior ou igual a 0."))
                .andExpect(jsonPath("$.erros[1].campo").value("tamanho"))
                .andExpect(jsonPath("$.erros[1].mensagem").value("O tamanho deve estar entre 1 e 100."));
    }

    @Test
    void somenteLeituraEntaoPostNaoEAceito() throws Exception {
        mockMvc.perform(post(RESUMO).with(admin(2)))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}
