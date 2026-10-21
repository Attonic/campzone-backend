package br.com.codeacle.campzone.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Login recusado. A mesma mensagem vale para e-mail inexistente e senha errada, para não revelar quais e-mails
 * existem. Vira 401 em ProblemDetail.
 */
@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "E-mail ou senha inválidos.")
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais inválidas");
    }
}
