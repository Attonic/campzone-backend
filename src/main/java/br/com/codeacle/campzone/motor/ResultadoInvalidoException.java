package br.com.codeacle.campzone.motor;

/** O resultado de um jogo viola uma regra (placar negativo, empate proibido, participante desconhecido...). */
public class ResultadoInvalidoException extends MotorException {

    private static final long serialVersionUID = 1L;

    public ResultadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
