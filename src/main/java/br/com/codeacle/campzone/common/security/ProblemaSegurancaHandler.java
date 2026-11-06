package br.com.codeacle.campzone.common.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Respostas de segurança em {@link ProblemDetail} (RFC 9457): 401 (sem token, token inválido ou expirado), 403
 * (papel sem permissão) e 400 (requisição barrada pelo firewall do Spring Security antes do DispatcherServlet).
 * Nenhuma mensagem de exceção interna é devolvida ao cliente, para não vazar detalhes do token.
 */
public class ProblemaSegurancaHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler, RequestRejectedHandler {

    private final JsonMapper jsonMapper;

    public ProblemaSegurancaHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException ex) throws IOException {
        // Token enviado mas rejeitado (malformado, assinatura errada, expirado): OAuth2AuthenticationException.
        boolean tokenInvalido = ex instanceof OAuth2AuthenticationException;
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                tokenInvalido ? "Bearer error=\"invalid_token\"" : "Bearer");
        escrever(response, HttpStatus.UNAUTHORIZED, "Não autenticado",
                tokenInvalido ? "Token inválido ou expirado." : "Autenticação necessária.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        escrever(response, HttpStatus.FORBIDDEN, "Acesso negado", "Sem permissão para este recurso.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            RequestRejectedException ex) throws IOException {
        escrever(response, HttpStatus.BAD_REQUEST, "Requisição inválida", "Requisição inválida.");
    }

    private void escrever(HttpServletResponse response, HttpStatus status, String titulo,
            String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(problema));
    }
}
