package br.com.codeacle.campzone.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Controller só de teste: exercita o advice e a SecurityConfig sem depender de módulos de negócio. */
@RestController
@RequestMapping("/api/v1")
public class ApiTesteController {

    record NomeRequest(@NotBlank(message = "nome é obrigatório") String nome) {
    }

    @GetMapping("/public/ping")
    String ping() {
        return "pong";
    }

    @PostMapping("/public/validar")
    String validar(@Valid @RequestBody NomeRequest request) {
        return request.nome();
    }

    @GetMapping("/public/explode")
    String explode() {
        throw new IllegalStateException("detalhe interno que não pode vazar");
    }

    @GetMapping("/privado")
    String privado() {
        return "segredo";
    }

    @GetMapping("/public/acesso-negado")
    String acessoNegado() {
        throw new org.springframework.security.access.AccessDeniedException("negado");
    }

    @GetMapping("/public/nao-autenticado")
    String naoAutenticado() {
        throw new org.springframework.security.authentication.BadCredentialsException("credenciais");
    }

    @GetMapping("/public/conflito")
    String conflito() {
        throw new ConflitoAnotadoException();
    }

    @org.springframework.web.bind.annotation.ResponseStatus(code = org.springframework.http.HttpStatus.CONFLICT,
            reason = "Recurso em conflito")
    static class ConflitoAnotadoException extends RuntimeException {
    }
}
