package br.com.codeacle.campzone.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Injeta no parâmetro ({@code Long}) do controller o id do usuário autenticado, lido do claim {@code sub} do JWT. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface UsuarioAtual {
}
