package br.com.codeacle.campzone.admin.api.response;

/**
 * Números gerais da plataforma. {@code totalUsuarios} soma organizadores e admins. Campeonato ativo é {@code RASCUNHO}
 * ou {@code EM_ANDAMENTO}.
 */
public record ResumoPlataformaResponse(
        long totalOrganizacoes,
        long totalUsuarios,
        long totalCampeonatos,
        long campeonatosAtivos) {
}
