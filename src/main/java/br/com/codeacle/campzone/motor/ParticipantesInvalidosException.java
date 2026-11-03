package br.com.codeacle.campzone.motor;

/** A lista de participantes não serve ao motor (menos de 2, ids repetidos, nome vazio...). */
public class ParticipantesInvalidosException extends MotorException {

    private static final long serialVersionUID = 1L;

    public ParticipantesInvalidosException(String mensagem) {
        super(mensagem);
    }
}
