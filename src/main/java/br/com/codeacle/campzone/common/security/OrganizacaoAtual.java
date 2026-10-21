package br.com.codeacle.campzone.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injeta no parâmetro ({@code Long}) do controller o id da organização do usuário autenticado, lido <b>somente</b> do
 * claim {@code org} do JWT validado: nunca do body, query, header ou path. Responde 403 se o token não tiver
 * organização (admin da plataforma).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface OrganizacaoAtual {
}
