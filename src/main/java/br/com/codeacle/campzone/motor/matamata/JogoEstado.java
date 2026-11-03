package br.com.codeacle.campzone.motor.matamata;

/**
 * Um jogo da chave.
 *
 * @param numeroJogo número do jogo no confronto, a partir de 1
 * @param mandanteId id do mandante, ou {@code null} enquanto o confronto aguarda os participantes
 * @param visitanteId id do visitante, ou {@code null} enquanto o confronto aguarda os participantes
 * @param situacao   situação do jogo, calculada a partir dos resultados
 */
public record JogoEstado(int numeroJogo, Long mandanteId, Long visitanteId, SituacaoJogo situacao) {
}
