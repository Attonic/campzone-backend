package br.com.codeacle.campzone.common.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolve {@link OrganizacaoAtual} e {@link UsuarioAtual} a partir do JWT do {@code SecurityContext}. Não lê nada da
 * requisição (body, query, header, path), por isso um cliente não consegue trocar de organização.
 */
@Component
public class UsuarioAutenticadoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return (parametro.hasParameterAnnotation(OrganizacaoAtual.class)
                || parametro.hasParameterAnnotation(UsuarioAtual.class))
                && Long.class.equals(parametro.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer container,
            NativeWebRequest requisicao, WebDataBinderFactory binderFactory) {
        UsuarioAutenticado usuario = UsuarioAutenticado.de(SecurityContextHolder.getContext().getAuthentication());
        return parametro.hasParameterAnnotation(OrganizacaoAtual.class)
                ? usuario.organizacaoIdObrigatorio()
                : usuario.id();
    }
}
