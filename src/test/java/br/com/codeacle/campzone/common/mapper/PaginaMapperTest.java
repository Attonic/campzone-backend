package br.com.codeacle.campzone.common.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.codeacle.campzone.common.api.response.PaginaResponse;

class PaginaMapperTest {

    private final PaginaMapper mapper = new PaginaMapper();

    @ParameterizedTest
    @CsvSource({ "0,20,0", "1,20,1", "20,20,1", "21,20,2", "40,20,2", "41,20,3", "1,1,1", "7,3,3" })
    void totalDePaginasArredondaParaCima(long totalItens, int tamanho, int totalPaginas) {
        PaginaResponse<String> pagina = mapper.toPaginaResponse(List.of(), 0, tamanho, totalItens);

        assertThat(pagina.totalPaginas()).isEqualTo(totalPaginas);
        assertThat(pagina.totalItens()).isEqualTo(totalItens);
    }

    @Test
    void devolveOQuePediuMesmoAlemDaUltimaPagina() {
        PaginaResponse<String> pagina = mapper.toPaginaResponse(List.of(), 9, 20, 3);

        assertThat(pagina.itens()).isEmpty();
        assertThat(pagina.pagina()).isEqualTo(9);
        assertThat(pagina.tamanho()).isEqualTo(20);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
    }

    @Test
    void copiaOsItens() {
        List<String> itens = new java.util.ArrayList<>(List.of("a", "b"));

        PaginaResponse<String> pagina = mapper.toPaginaResponse(itens, 0, 20, 2);
        itens.add("c");

        assertThat(pagina.itens()).containsExactly("a", "b");
    }

    @Test
    void naoPermiteTamanhoMenorQueUm() {
        assertThatThrownBy(() -> mapper.toPaginaResponse(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
