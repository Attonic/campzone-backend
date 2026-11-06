package br.com.codeacle.testes;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Controller só de teste (fora do component scan, como o {@link ApiTesteController}): exercita a validação de
 * parâmetros por {@code @Validated} e a diferença entre violação de parâmetro (400) e de bean (500).
 */
@RestController
@RequestMapping("/api/v1/public/teste-validacao")
@Validated
public class ApiTesteValidacaoController {

    record Bean(@NotBlank(message = "texto é obrigatório") String texto) {
    }

    @GetMapping("/parametros")
    String parametros(@RequestParam(defaultValue = "1") @Min(value = 1, message = "valor mínimo é 1")
            @Max(value = 10, message = "valor máximo é 10") int valor,
            @RequestParam(required = false) @Size(max = 3, message = "máximo de 3 caracteres") String texto) {
        return "ok";
    }

    /** Violação de <b>bean</b> (não de parâmetro): equivale a uma entidade inválida na persistência. */
    @GetMapping("/bean")
    String bean() {
        var violacoes = Validation.buildDefaultValidatorFactory().getValidator().validate(new Bean(""));
        throw new ConstraintViolationException(violacoes);
    }
}
