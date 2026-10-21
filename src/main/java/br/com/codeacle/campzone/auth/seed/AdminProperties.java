package br.com.codeacle.campzone.auth.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais do admin da plataforma ({@code campzone.admin.*}). Em dev vêm do {@code application-dev.yml}; em prod
 * de {@code ADMIN_EMAIL} e {@code ADMIN_SENHA}, obrigatórias.
 */
@ConfigurationProperties("campzone.admin")
public record AdminProperties(String email, String senha) {

    @Override
    public String toString() {
        return "AdminProperties[email=" + email + ", senha=***]";
    }
}
