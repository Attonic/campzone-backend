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
 * requisição (body, query, header, path), por isso um cliente não consegue trocar de organização. Aceita {@code Long}
 * e {@code long}; qualquer outro tipo é erro de programação (falha fechada, sem cair em {@code @RequestParam}).
 */
@Component
public class UsuarioAutenticadoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        // Qualquer parâmetro anotado é nosso, seja qual for o tipo. Se devolvêssemos false para um tipo não
        // suportado (long, Integer...), o Spring cairia no resolver de @RequestParam e leria a query string.
        return parametro.hasParameterAnnotation(OrganizacaoAtual.class)
                || parametro.hasParameterAnnotation(UsuarioAtual.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer container,
            NativeWebRequest requisicao, WebDataBinderFactory binderFactory) {
        Class<?> tipo = parametro.getParameterType();
        if (!Long.class.equals(tipo) && !long.class.equals(tipo)) {
            throw new IllegalStateException("@OrganizacaoAtual e @UsuarioAtual só suportam Long/long, mas o parâmetro "
                    + parametro.getParameterName() + " de " + parametro.getMethod() + " é " + tipo.getName() + ".");
        }
        UsuarioAutenticado usuario = UsuarioAutenticado.de(SecurityContextHolder.getContext().getAuthentication());
        return parametro.hasParameterAnnotation(OrganizacaoAtual.class)
                ? usuario.organizacaoIdObrigatorio()
                : usuario.id();
    }
}
