package br.com.codeacle.campzone.auth.api.request;

import br.com.codeacle.campzone.common.validation.TamanhoMaximoBytes;
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
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
        // O BCrypt limita a 72 BYTES (não caracteres): "ç" ocupa 2 bytes em UTF-8.
        @TamanhoMaximoBytes(value = 72, message = "A senha deve ter no máximo 72 bytes (caracteres acentuados contam 2).")
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
