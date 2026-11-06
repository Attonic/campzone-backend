package br.com.codeacle.campzone.common.error;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Substitui o {@code BasicErrorController} do Spring Boot: erros que o container encaminha para {@code /error}
 * (falhas fora do DispatcherServlet, como as de filtros) saem em ProblemDetail, nunca com mensagem ou stacktrace.
 */
@RestController
public class ProblemaErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ProblemDetail> erro(HttpServletRequest request) {
        HttpStatus status = statusDo(request);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status,
                status.is5xxServerError() ? "Erro interno inesperado." : status.getReasonPhrase());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }

    private static HttpStatus statusDo(HttpServletRequest request) {
        Object codigo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (codigo instanceof Integer valor) {
            HttpStatus status = HttpStatus.resolve(valor);
            if (status != null && status.isError()) {
                return status;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
