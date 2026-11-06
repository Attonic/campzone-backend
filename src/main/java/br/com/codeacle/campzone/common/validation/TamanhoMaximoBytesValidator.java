package br.com.codeacle.campzone.common.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TamanhoMaximoBytesValidator implements ConstraintValidator<TamanhoMaximoBytes, CharSequence> {

    private int maximo;

    @Override
    public void initialize(TamanhoMaximoBytes anotacao) {
        this.maximo = anotacao.value();
    }

    @Override
    public boolean isValid(CharSequence valor, ConstraintValidatorContext contexto) {
        return valor == null || valor.toString().getBytes(StandardCharsets.UTF_8).length <= maximo;
    }
}
