package br.com.codeacle.campzone.admin.service;

import br.com.codeacle.campzone.admin.api.response.OrganizacaoAdminResponse;
import br.com.codeacle.campzone.admin.api.response.ResumoPlataformaResponse;
import br.com.codeacle.campzone.common.api.response.PaginaResponse;

/**
 * Visão do admin da plataforma sobre todas as organizações (ADR 0002). É o único service que lê dados entre
 * organizações e só é usado atrás de {@code /api/v1/admin/**}. Somente leitura.
 */
public interface AdminPlataformaService {

    ResumoPlataformaResponse resumo();

    /**
     * Organizações da mais nova para a mais antiga. {@code pagina} começa em 0; {@code busca} (opcional) filtra por
     * nome contendo o texto, sem diferenciar maiúsculas.
     */
    PaginaResponse<OrganizacaoAdminResponse> listarOrganizacoes(int pagina, int tamanho, String busca);
}
