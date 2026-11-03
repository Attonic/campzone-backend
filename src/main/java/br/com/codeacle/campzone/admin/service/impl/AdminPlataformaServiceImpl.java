package br.com.codeacle.campzone.admin.service.impl;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.codeacle.campzone.admin.api.response.OrganizacaoAdminResponse;
import br.com.codeacle.campzone.admin.api.response.ResumoPlataformaResponse;
import br.com.codeacle.campzone.admin.mapper.AdminMapper;
import br.com.codeacle.campzone.admin.repository.AdminConsultaRepository;
import br.com.codeacle.campzone.admin.repository.OrganizacaoComContagens;
import br.com.codeacle.campzone.admin.service.AdminPlataformaService;
import br.com.codeacle.campzone.campeonato.domain.StatusCampeonato;
import br.com.codeacle.campzone.common.api.response.PaginaResponse;

@Service
public class AdminPlataformaServiceImpl implements AdminPlataformaService {

    /** Campeonato ativo é RASCUNHO ou EM_ANDAMENTO (o mesmo de {@code Campeonato.isAtivo()}; há teste que garante). */
    static final List<StatusCampeonato> STATUS_ATIVOS = List.of(StatusCampeonato.RASCUNHO,
            StatusCampeonato.EM_ANDAMENTO);

    private static final char ESCAPE = AdminConsultaRepository.ESCAPE_LIKE;

    private final AdminConsultaRepository repository;
    private final AdminMapper mapper;

    public AdminPlataformaServiceImpl(AdminConsultaRepository repository, AdminMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoPlataformaResponse resumo() {
        return mapper.toResumoResponse(
                repository.contarOrganizacoes(),
                repository.contarUsuarios(),
                repository.contarCampeonatos(),
                repository.contarCampeonatosPorStatus(nomesDosStatusAtivos()));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<OrganizacaoAdminResponse> listarOrganizacoes(int pagina, int tamanho, String busca) {
        String padrao = padraoDeBusca(busca);
        long totalItens = repository.contarOrganizacoesPorNome(padrao);
        // Além da última página não há o que buscar (e evita estourar o deslocamento em páginas absurdas).
        long inicio = (long) pagina * tamanho;
        List<OrganizacaoComContagens> organizacoes = inicio < totalItens
                ? repository.listarOrganizacoes(padrao, nomesDosStatusAtivos(), PageRequest.of(pagina, tamanho))
                : List.of();
        return mapper.toPaginaResponse(organizacoes, pagina, tamanho, totalItens);
    }

    private static List<String> nomesDosStatusAtivos() {
        return STATUS_ATIVOS.stream().map(Enum::name).toList();
    }

    /**
     * Padrão de {@code like} para "nome contendo o texto": sem texto, casa tudo; {@code %}, {@code _} e o próprio
     * caractere de escape do texto digitado valem como literais.
     */
    static String padraoDeBusca(String busca) {
        if (busca == null || busca.isBlank()) {
            return "%";
        }
        StringBuilder padrao = new StringBuilder("%");
        for (char c : busca.strip().toCharArray()) {
            if (c == '%' || c == '_' || c == ESCAPE) {
                padrao.append(ESCAPE);
            }
            padrao.append(c);
        }
        return padrao.append('%').toString();
    }
}
