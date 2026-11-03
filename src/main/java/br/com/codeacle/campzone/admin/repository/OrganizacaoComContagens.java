package br.com.codeacle.campzone.admin.repository;

import java.time.Instant;

/** Projeção de uma organização com o plano e as contagens, lida numa única consulta (sem N+1). */
public record OrganizacaoComContagens(
        Long id,
        String nome,
        String planoCodigo,
        String planoNome,
        long totalUsuarios,
        long totalCampeonatos,
        long campeonatosAtivos,
        Instant criadoEm) {
}
