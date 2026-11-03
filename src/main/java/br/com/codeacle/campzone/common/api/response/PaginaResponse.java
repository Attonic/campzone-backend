package br.com.codeacle.campzone.common.api.response;

import java.util.List;

/**
 * Página de resultados na API. É o formato único das listagens paginadas: o {@code Page} do Spring nunca sai pela
 * API. {@code pagina} começa em 0 e devolve exatamente o que foi pedido, mesmo além da última página (neste caso
 * {@code itens} vem vazio).
 */
public record PaginaResponse<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas) {
}
