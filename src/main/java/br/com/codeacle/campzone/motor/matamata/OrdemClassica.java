package br.com.codeacle.campzone.motor.matamata;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordem clássica das seeds na primeira fase de uma chave de {@code P} posições (potência de 2), gerada
 * recursivamente: a ordem de {@code P} põe cada seed {@code s} da ordem de {@code P/2} ao lado do seu oposto
 * {@code P + 1 - s}. Para {@code P = 8} a lista é {@code 1, 8, 4, 5, 2, 7, 3, 6}, ou seja, os confrontos
 * {@code 1x8, 4x5, 2x7, 3x6}.
 *
 * <p>Com isso, em cada metade, quarto, oitavo... da chave há uma das melhores seeds: as seeds 1 e 2 só se encontram
 * na final, as quatro primeiras só se encontram a partir da semifinal, e assim por diante.
 */
final class OrdemClassica {

    private OrdemClassica() {
    }

    /**
     * Seeds na ordem da chave: as posições {@code 2i} e {@code 2i+1} formam o confronto {@code i+1} da primeira fase.
     *
     * @param tamanho tamanho da chave, potência de 2 (pelo menos 1)
     */
    static List<Integer> seeds(int tamanho) {
        if (tamanho == 1) {
            return List.of(1);
        }
        List<Integer> expandida = new ArrayList<>(tamanho);
        for (int seed : seeds(tamanho / 2)) {
            expandida.add(seed);
            expandida.add(tamanho + 1 - seed);
        }
        return expandida;
    }
}
