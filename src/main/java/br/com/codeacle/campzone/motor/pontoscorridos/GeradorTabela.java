package br.com.codeacle.campzone.motor.pontoscorridos;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera a tabela round-robin pelo método do círculo, na construção canônica de de Werra.
 *
 * <p>Os participantes ocupam as posições {@code 0..c-1} de um círculo ({@code c = m - 1}, onde {@code m} é o
 * número de participantes arredondado para cima ao próximo par). Com número par, o último da ordem sorteada fica
 * fixo fora do círculo; com número ímpar, o "fixo" é a folga (bye) e ninguém fica de fora do círculo. Na rodada
 * {@code r} (a partir de 0):
 * <ul>
 *   <li>o fixo enfrenta a posição {@code r} (com número ímpar, a posição {@code r} folga);</li>
 *   <li>a posição {@code r-k} enfrenta a posição {@code r+k} (módulo {@code c}), para {@code k = 1..m/2-1}.</li>
 * </ul>
 *
 * <p>Mando: o fixo joga em casa nas rodadas pares; no par {@code (r-k, r+k)} o mandante é {@code r-k} se {@code k}
 * é ímpar e {@code r+k} se é par. Com isso, contra os demais do círculo, o participante da posição {@code i} é
 * mandante exatamente quando {@code (i - r) mod c} é par, o que alterna casa e fora a cada rodada. Cada
 * participante do círculo tem no máximo uma quebra (jogar em casa, ou fora, duas rodadas seguidas), e o fixo e o
 * da posição 0 não têm nenhuma. O total fica em {@code n-2} quebras por turno para {@code n} par (o mínimo
 * teórico) e em 0 para {@code n} ímpar, pois a folga cai exatamente onde a quebra estaria. Cada participante tem
 * jogos em casa e fora iguais (ímpar) ou diferindo em 1 (par).
 *
 * <p>Os turnos seguintes repetem as rodadas do primeiro: os pares invertem o mando e os ímpares o repetem.
 */
final class GeradorTabela {

    private GeradorTabela() {
    }

    /**
     * @param ordem ids na ordem sorteada (pelo menos 2)
     */
    static TabelaPontosCorridos gerar(int turnos, List<Long> ordem) {
        List<RodadaGerada> primeiroTurno = primeiroTurno(ordem);
        List<RodadaGerada> rodadas = new ArrayList<>();
        for (int turno = 1; turno <= turnos; turno++) {
            int deslocamento = (turno - 1) * primeiroTurno.size();
            for (RodadaGerada base : primeiroTurno) {
                int numero = deslocamento + base.numero();
                rodadas.add(turno % 2 == 1 ? base.repetida(numero, turno) : base.comMandoInvertido(numero, turno));
            }
        }
        return new TabelaPontosCorridos(rodadas);
    }

    private static List<RodadaGerada> primeiroTurno(List<Long> ordem) {
        int participantes = ordem.size();
        boolean impar = participantes % 2 != 0;
        int tamanhoPar = impar ? participantes + 1 : participantes;
        int circulo = tamanhoPar - 1;
        List<RodadaGerada> rodadas = new ArrayList<>(circulo);
        for (int r = 0; r < circulo; r++) {
            List<JogoGerado> jogos = new ArrayList<>();
            Long folga = null;
            long girante = ordem.get(r);
            if (impar) {
                folga = girante;
            } else {
                long fixo = ordem.get(participantes - 1);
                jogos.add(r % 2 == 0 ? new JogoGerado(fixo, girante) : new JogoGerado(girante, fixo));
            }
            for (int k = 1; k < tamanhoPar / 2; k++) {
                long antes = ordem.get(Math.floorMod(r - k, circulo));
                long depois = ordem.get(Math.floorMod(r + k, circulo));
                jogos.add(k % 2 == 1 ? new JogoGerado(antes, depois) : new JogoGerado(depois, antes));
            }
            rodadas.add(new RodadaGerada(r + 1, 1, jogos, folga));
        }
        return rodadas;
    }
}
