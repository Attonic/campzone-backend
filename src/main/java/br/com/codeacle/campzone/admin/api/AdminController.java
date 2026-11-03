package br.com.codeacle.campzone.admin.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.codeacle.campzone.admin.api.response.OrganizacaoAdminResponse;
import br.com.codeacle.campzone.admin.api.response.ResumoPlataformaResponse;
import br.com.codeacle.campzone.admin.service.AdminPlataformaService;
import br.com.codeacle.campzone.common.api.response.PaginaResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Área do admin da plataforma (ADR 0002): só leitura. A {@code SecurityConfig} exige o papel
 * {@code ADMIN_PLATAFORMA} em tudo sob {@code /api/v1/admin/**}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Validated
public class AdminController {

    private final AdminPlataformaService adminPlataformaService;

    public AdminController(AdminPlataformaService adminPlataformaService) {
        this.adminPlataformaService = adminPlataformaService;
    }

    @GetMapping("/resumo")
    public ResumoPlataformaResponse resumo() {
        return adminPlataformaService.resumo();
    }

    @GetMapping("/organizacoes")
    public PaginaResponse<OrganizacaoAdminResponse> listarOrganizacoes(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "A página deve ser maior ou igual a 0.")
            int pagina,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho deve estar entre 1 e 100.")
            @Max(value = 100, message = "O tamanho deve estar entre 1 e 100.")
            int tamanho,

            @RequestParam(required = false)
            @Size(max = 100, message = "A busca deve ter no máximo 100 caracteres.")
            String busca) {
        return adminPlataformaService.listarOrganizacoes(pagina, tamanho, busca);
    }
}
