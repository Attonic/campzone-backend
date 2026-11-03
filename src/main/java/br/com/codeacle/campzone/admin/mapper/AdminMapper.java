package br.com.codeacle.campzone.admin.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.codeacle.campzone.admin.api.response.OrganizacaoAdminResponse;
import br.com.codeacle.campzone.admin.api.response.ResumoPlataformaResponse;
import br.com.codeacle.campzone.admin.repository.OrganizacaoComContagens;
import br.com.codeacle.campzone.common.api.response.PaginaResponse;
import br.com.codeacle.campzone.common.mapper.PaginaMapper;

@Component
public class AdminMapper {

    private final PaginaMapper paginaMapper;

    public AdminMapper(PaginaMapper paginaMapper) {
        this.paginaMapper = paginaMapper;
    }

    public ResumoPlataformaResponse toResumoResponse(long totalOrganizacoes, long totalUsuarios,
            long totalCampeonatos, long campeonatosAtivos) {
        return new ResumoPlataformaResponse(totalOrganizacoes, totalUsuarios, totalCampeonatos, campeonatosAtivos);
    }

    public OrganizacaoAdminResponse toResponse(OrganizacaoComContagens organizacao) {
        return new OrganizacaoAdminResponse(
                organizacao.id(),
                organizacao.nome(),
                organizacao.planoCodigo(),
                organizacao.planoNome(),
                organizacao.totalUsuarios(),
                organizacao.totalCampeonatos(),
                organizacao.campeonatosAtivos(),
                organizacao.criadoEm());
    }

    public PaginaResponse<OrganizacaoAdminResponse> toPaginaResponse(List<OrganizacaoComContagens> organizacoes,
            int pagina, int tamanho, long totalItens) {
        List<OrganizacaoAdminResponse> itens = organizacoes.stream().map(this::toResponse).toList();
        return paginaMapper.toPaginaResponse(itens, pagina, tamanho, totalItens);
    }
}
