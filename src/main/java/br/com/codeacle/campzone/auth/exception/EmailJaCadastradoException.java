package br.com.codeacle.campzone.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Já existe usuário com o e-mail informado (a unicidade é global). Vira 409 em ProblemDetail. */
@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Já existe um usuário cadastrado com este e-mail.")
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("E-mail já cadastrado");
    }
}
