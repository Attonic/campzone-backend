package br.com.codeacle.campzone.motor.matamata;

import br.com.codeacle.campzone.motor.ResultadoInvalidoException;

/**
 * Resultado de um jogo da chave, identificado pela posição na chave e não pelos participantes: o mesmo par pode se
 * enfrentar várias vezes numa série. O placar segue o mandante e o visitante <strong>daquele jogo</strong>, como
 * informa o {@link JogoEstado}.
 *
 * <p>Prorrogação e pênaltis só entram quando o desempate chegou a eles. Os valores da prorrogação são os gols
 * marcados <em>na</em> prorrogação (não o acumulado com o tempo normal). Na ida e volta, os dois ficam no jogo 2.
 *
 * @param ordemFase             ordem da fase, como em {@link FaseEstado#ordem()}
 * @param posicao               posição do confronto na fase, a partir de 1
 * @param numeroJogo            número do jogo no confronto, a partir de 1
 * @param placarMandante        placar do mandante do jogo no tempo normal
 * @param placarVisitante       placar do visitante do jogo no tempo normal
 * @param prorrogacaoMandante   gols do mandante na prorrogação, ou {@code null} se não houve
 * @param prorrogacaoVisitante  gols do visitante na prorrogação, ou {@code null} se não houve
 * @param penaltisMandante      pênaltis convertidos pelo mandante, ou {@code null} se não houve
 * @param penaltisVisitante     pênaltis convertidos pelo visitante, ou {@code null} se não houve
 */
public record ResultadoMataMata(int ordemFase, int posicao, int numeroJogo, int placarMandante, int placarVisitante,
        Integer prorrogacaoMandante, Integer prorrogacaoVisitante, Integer penaltisMandante,
        Integer penaltisVisitante) {

    /**
     * Valida a chave, o placar e os pares de prorrogação e de pênaltis.
     *
     * @throws ResultadoInvalidoException se algum valor for negativo ou se só um lado da prorrogação (ou dos
     *                                    pênaltis) vier informado
     */
    public ResultadoMataMata {
        if (ordemFase < 0 || posicao < 0 || numeroJogo < 0) {
            throw new ResultadoInvalidoException("A posição do jogo na chave não pode ser negativa (fase " + ordemFase
                    + ", confronto " + posicao + ", jogo " + numeroJogo + ")");
        }
        if (placarMandante < 0 || placarVisitante < 0) {
            throw new ResultadoInvalidoException(
                    "O placar não pode ser negativo (" + placarMandante + " x " + placarVisitante + ")");
        }
        exigirParValido("prorrogação", prorrogacaoMandante, prorrogacaoVisitante);
        exigirParValido("pênaltis", penaltisMandante, penaltisVisitante);
    }

    /** Se o jogo traz a prorrogação (os dois lados). */
    boolean temProrrogacao() {
        return prorrogacaoMandante != null;
    }

    /** Se o jogo traz os pênaltis (os dois lados). */
    boolean temPenaltis() {
        return penaltisMandante != null;
    }

    private static void exigirParValido(String nome, Integer mandante, Integer visitante) {
        if ((mandante == null) != (visitante == null)) {
            throw new ResultadoInvalidoException("Informe os dois lados da " + nome + " ou nenhum");
        }
        if (mandante != null && (mandante < 0 || visitante < 0)) {
            throw new ResultadoInvalidoException("A " + nome + " não pode ser negativa (" + mandante + " x " + visitante + ")");
        }
    }
}
