package br.com.codeacle.campzone.motor;

/**
 * Resultado de um jogo já disputado. "Placar" é genérico: gols, pontos, sets ou partidas, conforme a modalidade.
 *
 * @throws ResultadoInvalidoException se o placar for negativo ou se mandante e visitante forem o mesmo participante
 */
public record ResultadoJogo(long mandanteId, long visitanteId, int placarMandante, int placarVisitante) {

    public ResultadoJogo {
        if (mandanteId == visitanteId) {
            throw new ResultadoInvalidoException("Um participante não pode jogar contra si mesmo (id " + mandanteId + ")");
        }
        if (placarMandante < 0 || placarVisitante < 0) {
            throw new ResultadoInvalidoException(
                    "O placar não pode ser negativo (" + placarMandante + " x " + placarVisitante + ")");
        }
    }
}
