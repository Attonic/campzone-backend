package br.com.codeacle.campzone.common.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.codeacle.campzone.common.api.response.PaginaResponse;

/** Monta a {@link PaginaResponse} genérica a partir dos itens já convertidos e dos totais. */
@Component
public class PaginaMapper {

    public <T> PaginaResponse<T> toPaginaResponse(List<T> itens, int pagina, int tamanho, long totalItens) {
        if (tamanho < 1) {
            throw new IllegalArgumentException("O tamanho da página deve ser pelo menos 1.");
        }
        long totalPaginas = (totalItens + tamanho - 1) / tamanho;
        return new PaginaResponse<>(List.copyOf(itens), pagina, tamanho, totalItens,
                (int) Math.min(totalPaginas, Integer.MAX_VALUE));
    }
}
