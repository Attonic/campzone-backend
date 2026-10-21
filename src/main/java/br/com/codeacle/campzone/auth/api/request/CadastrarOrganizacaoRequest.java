package br.com.codeacle.campzone.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro de uma organização junto com o seu primeiro usuário (organizador). */
public record CadastrarOrganizacaoRequest(
        @NotBlank(message = "O nome da organização é obrigatório.")
        @Size(min = 2, max = 100, message = "O nome da organização deve ter entre 2 e 100 caracteres.")
        String nomeOrganizacao,

        @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail é inválido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha) {

    /** Espaços nas pontas do e-mail (colagem) não devem invalidar nem diferenciar o cadastro. */
    public CadastrarOrganizacaoRequest {
        email = email == null ? null : email.strip();
    }

    /** Evita a senha no log caso o record seja impresso. */
    @Override
    public String toString() {
        return "CadastrarOrganizacaoRequest[nomeOrganizacao=" + nomeOrganizacao + ", nome=" + nome
                + ", email=" + email + ", senha=***]";
    }
}
