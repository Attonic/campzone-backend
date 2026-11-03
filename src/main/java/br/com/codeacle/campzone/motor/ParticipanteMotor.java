package br.com.codeacle.campzone.motor;

/**
 * Participante visto pelo motor. O {@code id} é o do banco (o motor nunca inventa ids) e o {@code seed} é a posição
 * de cabeça de chave, opcional e usada só pelo mata-mata.
 */
public record ParticipanteMotor(long id, String nome, Integer seed) {

    /**
     * Valida o nome do participante.
     *
     * @throws ParticipantesInvalidosException se o nome for nulo ou vazio
     */
    public ParticipanteMotor {
        if (nome == null || nome.isBlank()) {
            throw new ParticipantesInvalidosException("O participante " + id + " precisa de um nome");
        }
    }
}
