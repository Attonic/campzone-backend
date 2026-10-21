package br.com.codeacle.campzone.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import br.com.codeacle.campzone.common.error.GlobalExceptionHandler;
import br.com.codeacle.campzone.common.security.JwtConfig;
import br.com.codeacle.campzone.common.security.SecurityConfig;
import br.com.codeacle.campzone.common.security.UsuarioAutenticadoArgumentResolver;
import br.com.codeacle.campzone.common.security.WebSegurancaConfig;

/**
 * Configuração de segurança real (cadeia de filtros, JWT, resolver de {@code @OrganizacaoAtual}, advice de erros)
 * para os {@code @WebMvcTest}. Usar junto com {@code @WebMvcTest(controllers = ...)}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({ SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class, WebSegurancaConfig.class,
        UsuarioAutenticadoArgumentResolver.class })
@TestPropertySource(properties = "campzone.jwt.segredo=" + TokensDeTeste.SEGREDO)
public @interface SegurancaWebTest {
}
