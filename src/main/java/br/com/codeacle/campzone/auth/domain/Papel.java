package br.com.codeacle.campzone.auth.domain;

/**
 * Papel de acesso do usuário (ADR 0002). Persistido como texto na entidade {@link Usuario} (ADR 0003), então novos
 * valores podem ser adicionados sem alterar o schema.
 */
public enum Papel {

    /** Administra a plataforma inteira; não pertence a nenhuma organização e só acessa {@code /api/v1/admin/**}. */
    ADMIN_PLATAFORMA,

    /** Dono de exatamente uma organização (tenant); acessa as rotas de organização. */
    ORGANIZADOR;

    /** Nome da autoridade no Spring Security (prefixo {@code ROLE_}). */
    public String autoridade() {
        return "ROLE_" + name();
    }
}
