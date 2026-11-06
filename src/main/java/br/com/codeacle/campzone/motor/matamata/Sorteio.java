package br.com.codeacle.campzone.motor.matamata;

import java.util.Random;

/**
 * Sorteio de desempate do mata-mata (ADR 0005, Adendo A, item 3). É por confronto e depende só da seed do campeonato
 * e da posição do confronto na chave, por isso é reprodutível e não muda com a ordem em que os resultados chegam.
 *
 * <p>A semente do {@code Random} passa antes pelo finalizador de 64 bits do SplitMix64. Sem ele, confrontos vizinhos
 * da mesma fase têm sementes consecutivas, e o primeiro {@code nextBoolean} do LCG do {@code Random} é quase sempre o
 * mesmo para sementes assim (coincidiam em 99,9% dos casos, contra 50% esperado).
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
        return new Random(splitMix64(seedSorteio + PASSO_POR_FASE * ordemFase + posicao)).nextBoolean();
    }

    /** Finalizador de 64 bits do SplitMix64: espalha sementes vizinhas por todos os bits. */
    private static long splitMix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
