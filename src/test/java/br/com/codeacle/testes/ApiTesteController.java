package br.com.codeacle.testes;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.codeacle.campzone.common.security.OrganizacaoAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Controller só de teste: exercita o advice e a SecurityConfig sem depender de módulos de negócio. Fica fora do
 * pacote base {@code br.com.codeacle.campzone} de propósito: assim o component scan da aplicação (inclusive em
 * {@code @SpringBootTest} e no {@code TestCampzoneApplication}) nunca o registra. Os web tests o importam
 * explicitamente com {@code @Import}.
 */
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

    @GetMapping("/admin/teste")
    String areaAdmin() {
        return "admin";
    }

    /** Devolve a organização resolvida do JWT, para provar que body, query e header são ignorados. */
    @GetMapping("/teste/organizacao-atual")
    Long organizacaoAtual(@OrganizacaoAtual Long organizacaoId) {
        return organizacaoId;
    }

    /** Primitivo: sem o resolver cobrir {@code long}, o Spring leria {@code ?organizacaoId=} da query. */
    @GetMapping("/teste/organizacao-atual-primitivo")
    long organizacaoAtualPrimitivo(@OrganizacaoAtual long organizacaoId) {
        return organizacaoId;
    }

    /** Tipo não suportado: deve falhar, nunca ler da query. */
    @GetMapping("/teste/organizacao-atual-texto")
    String organizacaoAtualTexto(@OrganizacaoAtual String organizacaoId) {
        return organizacaoId;
    }

    @GetMapping("/teste/organizacao-atual-inteiro")
    String organizacaoAtualInteiro(@OrganizacaoAtual Integer organizacaoId) {
        return String.valueOf(organizacaoId);
    }

    @PostMapping("/teste/organizacao-atual")
    Long organizacaoAtualComBody(@OrganizacaoAtual Long organizacaoId, @RequestBody Map<String, Object> corpo) {
        return organizacaoId;
    }

    @GetMapping("/public/acesso-negado")
    String acessoNegado() {
        throw new org.springframework.security.access.AccessDeniedException("negado");
    }

    @GetMapping("/public/nao-autenticado")
    String naoAutenticado() {
        throw new org.springframework.security.authentication.BadCredentialsException("credenciais");
    }

    @GetMapping("/public/invariante")
    String invariante() {
        throw new br.com.codeacle.campzone.common.domain.InvarianteDominioException("Nome é obrigatório.");
    }

    @GetMapping("/public/conflito")
    String conflito() {
        throw new ConflitoAnotadoException();
    }

    @org.springframework.web.bind.annotation.ResponseStatus(code = org.springframework.http.HttpStatus.CONFLICT,
            reason = "Recurso em conflito")
    static class ConflitoAnotadoException extends RuntimeException {
    }

    @GetMapping("/public/indisponivel")
    String indisponivel() {
        throw new IndisponivelAnotadoException();
    }

    @org.springframework.web.bind.annotation.ResponseStatus(code = org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
    static class IndisponivelAnotadoException extends RuntimeException {
        IndisponivelAnotadoException() {
            super("falha-5xx-anotada");
        }
    }
}
