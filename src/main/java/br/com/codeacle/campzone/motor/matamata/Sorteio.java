package br.com.codeacle.campzone.motor.matamata;

import java.util.Random;

/**
 * Sorteio de desempate do mata-mata (ADR 0005, Adendo A, item 3). É por confronto e depende só da seed do campeonato
 * e da posição do confronto na chave, por isso é reprodutível e não muda com a ordem em que os resultados chegam.
 */
final class Sorteio {

    /** Passo entre as fases, para os confrontos de fases diferentes não compartilharem a semente. */
    private static final long PASSO_POR_FASE = 1_000_003L;

    private Sorteio() {
    }

    /**
     * Se o sorteio do confronto favorece o participante A (a melhor seed).
     *
     * @param ordemFase ordem da fase do confronto, como em {@link FaseEstado#ordem()}
     * @param posicao   posição do confronto na fase
     */
    static boolean favoreceA(long seedSorteio, int ordemFase, int posicao) {
        return new Random(seedSorteio + PASSO_POR_FASE * ordemFase + posicao).nextBoolean();
    }
}
