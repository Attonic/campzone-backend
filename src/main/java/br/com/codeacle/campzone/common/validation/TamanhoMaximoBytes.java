package br.com.codeacle.campzone.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * O texto codificado em UTF-8 não pode passar de {@code value} bytes. Diferente de {@code @Size}, que conta
 * caracteres: serve para limites de bytes, como os 72 do BCrypt. {@code null} é válido (use {@code @NotBlank}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Constraint(validatedBy = TamanhoMaximoBytesValidator.class)
public @interface TamanhoMaximoBytes {

    int value();

    String message() default "Deve ter no máximo {value} bytes (UTF-8).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
