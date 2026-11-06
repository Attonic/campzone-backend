package br.com.codeacle.campzone.admin.api.response;

import java.time.Instant;

/** Organização como o admin da plataforma a vê: plano e contagens. */
public record OrganizacaoAdminResponse(
        Long id,
        String nome,
        String planoCodigo,
        String planoNome,
        long totalUsuarios,
        long totalCampeonatos,
        long campeonatosAtivos,
        Instant criadoEm) {
}
