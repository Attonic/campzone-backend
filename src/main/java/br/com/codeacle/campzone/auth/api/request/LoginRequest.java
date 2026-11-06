package br.com.codeacle.campzone.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String senha) {

    /** Espaços nas pontas do e-mail (colagem) não devem invalidar o login. */
    public LoginRequest {
        email = email == null ? null : email.strip();
    }

    /** Evita a senha no log caso o record seja impresso. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", senha=***]";
    }
}
