package br.com.codeacle.campzone.admin.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.admin.api.response.OrganizacaoAdminResponse;
import br.com.codeacle.campzone.admin.api.response.ResumoPlataformaResponse;
import br.com.codeacle.campzone.admin.repository.OrganizacaoComContagens;
import br.com.codeacle.campzone.common.api.response.PaginaResponse;
import br.com.codeacle.campzone.common.mapper.PaginaMapper;

class AdminMapperTest {

    private static final Instant CRIADA_EM = Instant.parse("2026-10-21T20:00:00Z");

    private final AdminMapper mapper = new AdminMapper(new PaginaMapper());

    @Test
    void mapeiaOResumo() {
        assertThat(mapper.toResumoResponse(1, 2, 3, 4)).isEqualTo(new ResumoPlataformaResponse(1, 2, 3, 4));
    }

    @Test
    void mapeiaAOrganizacaoCampoACampo() {
        OrganizacaoComContagens origem = new OrganizacaoComContagens(7L, "Liga", "GRATIS", "Grátis", 2, 5, 3,
                CRIADA_EM);

        assertThat(mapper.toResponse(origem))
                .isEqualTo(new OrganizacaoAdminResponse(7L, "Liga", "GRATIS", "Grátis", 2, 5, 3, CRIADA_EM));
    }

    @Test
    void mapeiaAPaginaMantendoAOrdem() {
        OrganizacaoComContagens a = new OrganizacaoComContagens(2L, "B", "GRATIS", "Grátis", 0, 0, 0, CRIADA_EM);
        OrganizacaoComContagens b = new OrganizacaoComContagens(1L, "A", "GRATIS", "Grátis", 1, 1, 1, CRIADA_EM);

        PaginaResponse<OrganizacaoAdminResponse> pagina = mapper.toPaginaResponse(List.of(a, b), 0, 2, 3);

        assertThat(pagina.itens()).extracting(OrganizacaoAdminResponse::id).containsExactly(2L, 1L);
        assertThat(pagina.totalItens()).isEqualTo(3);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
    }
}
