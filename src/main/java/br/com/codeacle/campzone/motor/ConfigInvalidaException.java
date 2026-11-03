package br.com.codeacle.campzone.motor;

/** A configuração do formato viola uma regra (pontos incoerentes, critérios repetidos, turnos &lt; 1...). */
public class ConfigInvalidaException extends MotorException {

    private static final long serialVersionUID = 1L;

    public ConfigInvalidaException(String mensagem) {
        super(mensagem);
    }
}
