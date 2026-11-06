package br.com.codeacle.campzone.common.error;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.codeacle.campzone.common.domain.InvarianteDominioException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

/**
 * Traduz erros em {@link ProblemDetail} (RFC 9457). Os erros padrão do Spring MVC (404, 405, 415...)
 * já são tratados pela classe base; aqui detalhamos a validação e evitamos vazar detalhes em erros inesperados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<CampoInvalido> erros = ex.getBindingResult().getAllErrors().stream()
                .map(erro -> new CampoInvalido(
                        erro instanceof FieldError fieldError ? fieldError.getField() : erro.getObjectName(),
                        erro.getDefaultMessage()))
                .toList();

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Um ou mais campos são inválidos.");
        problema.setTitle("Erro de validação");
        problema.setProperty("erros", erros);
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Parâmetro de método inválido (query, path ou header com {@code @Min}, {@code @Size}... num controller
     * {@code @Validated}): responde 400 com os campos, no mesmo formato da validação de body. Só trata a violação de
     * <b>parâmetro</b>: uma violação de bean (ex.: entidade na persistência) é bug do servidor e continua 500.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleViolacaoDeParametro(ConstraintViolationException ex) {
        boolean soParametros = !ex.getConstraintViolations().isEmpty() && ex.getConstraintViolations().stream()
                .allMatch(violacao -> violacao.getExecutableParameters() != null);
        if (!soParametros) {
            return handleErroInesperado(ex);
        }
        List<CampoInvalido> erros = ex.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::paraCampoInvalido)
                .sorted(Comparator.comparing(CampoInvalido::campo).thenComparing(CampoInvalido::mensagem))
                .toList();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Um ou mais parâmetros são inválidos.");
        problema.setTitle("Erro de validação");
        problema.setProperty("erros", erros);
        return problema;
    }

    /** O nome do campo é o último nó do caminho (o parâmetro), sem o nome do método. */
    private static CampoInvalido paraCampoInvalido(ConstraintViolation<?> violacao) {
        String campo = "parametro";
        for (Path.Node no : violacao.getPropertyPath()) {
            campo = no.getName();
        }
        return new CampoInvalido(campo, violacao.getMessage());
    }

    /**
     * Invariante de domínio violada (ex.: nome só com espaços) é entrada inválida que passou pela validação da
     * borda: responde 400. A mensagem é escrita por nós nas entidades, então é segura para o cliente.
     */
    @ExceptionHandler(InvarianteDominioException.class)
    public ProblemDetail handleInvarianteDominio(InvarianteDominioException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Dado inválido");
        return problema;
    }

    /**
     * Exceções de segurança são relançadas para o Spring Security (ExceptionTranslationFilter) responder
     * 401/403 com os handlers configurados na SecurityConfig, em vez de virarem 500 aqui.
     */
    @ExceptionHandler({ AccessDeniedException.class, AuthenticationException.class })
    public void relancarExcecaoDeSeguranca(RuntimeException ex) {
        throw ex;
    }

    /** Exceções anotadas com {@link ResponseStatus} mantêm o status declarado, agora em ProblemDetail. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleErroInesperado(Exception ex) {
        ResponseStatus anotacao = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        if (anotacao != null) {
            HttpStatus status = anotacao.code();
            if (status.is5xxServerError()) {
                log.error("Erro de servidor ({})", status.value(), ex);
            }
            String detalhe = anotacao.reason().isEmpty() ? status.getReasonPhrase() : anotacao.reason();
            return ProblemDetail.forStatusAndDetail(status, detalhe);
        }
        log.error("Erro inesperado", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado.");
        problema.setTitle("Erro interno");
        return problema;
    }

    /** Campo que falhou na validação e a mensagem correspondente. */
    public record CampoInvalido(String campo, String mensagem) {
    }
}
