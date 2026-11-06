package br.com.codeacle.campzone.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import br.com.codeacle.campzone.admin.api.response.OrganizacaoAdminResponse;
import br.com.codeacle.campzone.admin.api.response.ResumoPlataformaResponse;
import br.com.codeacle.campzone.admin.mapper.AdminMapper;
import br.com.codeacle.campzone.admin.repository.AdminConsultaRepository;
import br.com.codeacle.campzone.admin.repository.OrganizacaoComContagens;
import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.campeonato.domain.ConfigPontosCorridosEmbutida;
import br.com.codeacle.campzone.campeonato.domain.StatusCampeonato;
import br.com.codeacle.campzone.common.api.response.PaginaResponse;
import br.com.codeacle.campzone.common.mapper.PaginaMapper;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.plano.domain.Plano;

class AdminPlataformaServiceImplTest {

    private static final Instant CRIADA_EM = Instant.parse("2026-10-21T20:00:00Z");

    private final AdminConsultaRepository repository = mock(AdminConsultaRepository.class);

    private AdminPlataformaServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new AdminPlataformaServiceImpl(repository, new AdminMapper(new PaginaMapper()));
    }

    private static OrganizacaoComContagens organizacao(long id, String nome) {
        return new OrganizacaoComContagens(id, nome, "GRATIS", "Grátis", 2, 5, 3, CRIADA_EM);
    }

    // --- resumo ---

    @Test
    void resumoJuntaAsQuatroContagensEContaSoRascunhoEEmAndamentoComoAtivos() {
        when(repository.contarOrganizacoes()).thenReturn(3L);
        when(repository.contarUsuarios()).thenReturn(5L);
        when(repository.contarCampeonatos()).thenReturn(11L);
        when(repository.contarCampeonatosPorStatus(anyCollection())).thenReturn(7L);

        ResumoPlataformaResponse resumo = service.resumo();

        assertThat(resumo).isEqualTo(new ResumoPlataformaResponse(3, 5, 11, 7));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> status = ArgumentCaptor.forClass(Collection.class);
        verify(repository).contarCampeonatosPorStatus(status.capture());
        assertThat(status.getValue()).containsExactlyInAnyOrder("RASCUNHO", "EM_ANDAMENTO");
    }

    @Test
    void statusAtivosSaoOsMesmosDeCampeonatoIsAtivo() {
        Organizacao organizacao = new Organizacao("Org", new Plano("GRATIS", "Grátis", 1, 8));
        Campeonato campeonato = Campeonato.pontosCorridos(organizacao, "Liga", "Futebol", "liga",
                ConfigPontosCorridosEmbutida.padrao(1));
        List<StatusCampeonato> ativosNoDominio = new ArrayList<>();
        if (campeonato.isAtivo()) {
            ativosNoDominio.add(campeonato.getStatus());
        }
        campeonato.iniciar();
        if (campeonato.isAtivo()) {
            ativosNoDominio.add(campeonato.getStatus());
        }
        campeonato.finalizar();
        if (campeonato.isAtivo()) {
            ativosNoDominio.add(campeonato.getStatus());
        }

        assertThat(AdminPlataformaServiceImpl.STATUS_ATIVOS).containsExactlyInAnyOrderElementsOf(ativosNoDominio);
        assertThat(StatusCampeonato.values()).hasSize(3);
    }

    // --- listagem ---

    @Test
    void listaMapeiaAsOrganizacoesEOsTotais() {
        when(repository.contarOrganizacoesPorNome("%")).thenReturn(45L);
        when(repository.listarOrganizacoes(eq("%"), anyCollection(), any(Pageable.class)))
                .thenReturn(List.of(organizacao(9, "Nove"), organizacao(8, "Oito")));

        PaginaResponse<OrganizacaoAdminResponse> pagina = service.listarOrganizacoes(1, 20, null);

        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamanho()).isEqualTo(20);
        assertThat(pagina.totalItens()).isEqualTo(45);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
        assertThat(pagina.itens()).containsExactly(
                new OrganizacaoAdminResponse(9L, "Nove", "GRATIS", "Grátis", 2, 5, 3, CRIADA_EM),
                new OrganizacaoAdminResponse(8L, "Oito", "GRATIS", "Grátis", 2, 5, 3, CRIADA_EM));
    }

    @Test
    void pedeAoRepositorioAPaginaCertaESemOrdenacaoExtra() {
        when(repository.contarOrganizacoesPorNome("%")).thenReturn(100L);
        when(repository.listarOrganizacoes(any(), anyCollection(), any())).thenReturn(List.of());

        service.listarOrganizacoes(2, 15, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).listarOrganizacoes(eq("%"), anyCollection(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(15);
        assertThat(pageable.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void paginaAlemDaUltimaDevolveVaziaSemConsultarALista() {
        when(repository.contarOrganizacoesPorNome("%")).thenReturn(40L);

        PaginaResponse<OrganizacaoAdminResponse> pagina = service.listarOrganizacoes(2, 20, null);

        assertThat(pagina.itens()).isEmpty();
        assertThat(pagina.pagina()).isEqualTo(2);
        assertThat(pagina.totalItens()).isEqualTo(40);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
        verify(repository, never()).listarOrganizacoes(any(), any(), any());
    }

    @Test
    void paginaAbsurdaNaoEstouraODeslocamentoNemConsultaALista() {
        when(repository.contarOrganizacoesPorNome("%")).thenReturn(3L);

        PaginaResponse<OrganizacaoAdminResponse> pagina = service.listarOrganizacoes(Integer.MAX_VALUE, 100, null);

        assertThat(pagina.itens()).isEmpty();
        assertThat(pagina.pagina()).isEqualTo(Integer.MAX_VALUE);
        assertThat(pagina.totalItens()).isEqualTo(3);
        verify(repository, never()).listarOrganizacoes(any(), any(), any());
    }

    @Test
    void semOrganizacoesDevolvePaginaVazia() {
        when(repository.contarOrganizacoesPorNome(anyString())).thenReturn(0L);

        PaginaResponse<OrganizacaoAdminResponse> pagina = service.listarOrganizacoes(0, 20, "nada");

        assertThat(pagina).isEqualTo(new PaginaResponse<>(List.of(), 0, 20, 0, 0));
        verify(repository, never()).listarOrganizacoes(any(), any(), any());
    }

    @Test
    void passaOsStatusAtivosParaAListagem() {
        when(repository.contarOrganizacoesPorNome("%")).thenReturn(1L);
        when(repository.listarOrganizacoes(any(), anyCollection(), any())).thenReturn(List.of());

        service.listarOrganizacoes(0, 20, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> status = ArgumentCaptor.forClass(Collection.class);
        verify(repository).listarOrganizacoes(eq("%"), status.capture(), any());
        assertThat(status.getValue()).containsExactlyInAnyOrder("RASCUNHO", "EM_ANDAMENTO");
    }

    @Test
    void buscaVaiComOMesmoPadraoParaContarEListar() {
        when(repository.contarOrganizacoesPorNome("%liga%")).thenReturn(1L);
        when(repository.listarOrganizacoes(eq("%liga%"), anyCollection(), any()))
                .thenReturn(List.of(organizacao(1, "Liga")));

        PaginaResponse<OrganizacaoAdminResponse> pagina = service.listarOrganizacoes(0, 20, "  liga ");

        assertThat(pagina.itens()).hasSize(1);
        verify(repository).contarOrganizacoesPorNome("%liga%");
    }

    // --- padrão de busca ---

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t\n" })
    void buscaVaziaCasaTudo(String busca) {
        assertThat(AdminPlataformaServiceImpl.padraoDeBusca(busca)).isEqualTo("%");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Liga|%Liga%",
            "  Liga dos Amigos |%Liga dos Amigos%",
            "100%|%100!%%",
            "time_a|%time!_a%",
            "ola!|%ola!!%",
            "%_!|%!%!_!!%" })
    void curingasEEscapeViramLiterais(String busca, String padrao) {
        assertThat(AdminPlataformaServiceImpl.padraoDeBusca(busca)).isEqualTo(padrao);
    }
}
