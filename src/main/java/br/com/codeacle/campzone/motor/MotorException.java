package br.com.codeacle.campzone.motor;

/**
 * Erro de regra do motor de competição (não verificada). A camada de aplicação converte as subclasses em respostas
 * de erro; o motor em si não conhece HTTP.
 */
public class MotorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MotorException(String mensagem) {
        super(mensagem);
    }
}
