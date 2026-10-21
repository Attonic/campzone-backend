package br.com.codeacle.campzone.common.security;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra o resolver de {@link OrganizacaoAtual} e {@link UsuarioAtual} no Spring MVC. */
@Component
public class WebSegurancaConfig implements WebMvcConfigurer {

    private final UsuarioAutenticadoArgumentResolver resolver;

    public WebSegurancaConfig(UsuarioAutenticadoArgumentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}
