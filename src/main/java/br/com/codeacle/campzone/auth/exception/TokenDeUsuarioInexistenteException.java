package br.com.codeacle.campzone.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * O token é válido, mas o usuário dele não existe mais. O token deixa de valer: 401 em ProblemDetail, com o mesmo
 * {@code detail} de um token inválido ou expirado.
 */
@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Token inválido ou expirado.")
public class TokenDeUsuarioInexistenteException extends RuntimeException {

    public TokenDeUsuarioInexistenteException() {
        super("Usuário do token não existe");
    }
}
