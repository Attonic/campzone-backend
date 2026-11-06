package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.sorteioFavoreceA;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

class SorteioTest {

    /** Quantas seeds de campeonato entram nos testes estatísticos (determinísticas: 0 a 9999). */
    private static final int SEEDS = 10_000;

    @Test
    void segueAFormulaDoAdendoDoAdr0005ComSplitMix64() {
        for (long seed : new long[] { 0, 1, 42, 20260, -7, Long.MAX_VALUE, Long.MIN_VALUE }) {
            for (int ordemFase = 1; ordemFase <= 6; ordemFase++) {
                for (int posicao = 1; posicao <= 8; posicao++) {
                    assertThat(Sorteio.favoreceA(seed, ordemFase, posicao)).as("seed %d, fase %d, confronto %d", seed,
                            ordemFase, posicao).isEqualTo(sorteioFavoreceA(seed, ordemFase, posicao));
                }
            }
        }
    }

    @Test
    void ePuroEReprodutivel() {
        for (int i = 0; i < 5; i++) {
            assertThat(Sorteio.favoreceA(77L, 3, 2)).isEqualTo(Sorteio.favoreceA(77L, 3, 2));
        }
    }

    /** Trava a saída: o SplitMix64 e o algoritmo de {@code Random(seed)} são fixos, então estes valores não mudam. */
    @Test
    void valoresTravados() {
        assertThat(Sorteio.favoreceA(42L, 1, 1)).isTrue();
        assertThat(Sorteio.favoreceA(42L, 2, 1)).isTrue();
        assertThat(Sorteio.favoreceA(42L, 2, 2)).isFalse();
        assertThat(Sorteio.favoreceA(42L, 3, 3)).isTrue();
        assertThat(Sorteio.favoreceA(20260L, 1, 4)).isFalse();
        assertThat(Sorteio.favoreceA(-5L, 2, 1)).isFalse();
        assertThat(Sorteio.favoreceA(0L, 3, 4)).isTrue();
    }

    // ---------------------------------------------------------------- estatística (determinística)

    /**
     * Pega a regressão que a revisão da 04 achou: com {@code new Random(soma)} direto, confrontos vizinhos da mesma fase
     * tinham sementes consecutivas e coincidiam em 99,9% dos casos. Com o SplitMix64 a taxa fica perto de 50%. As
     * seeds são fixas (0 a 9999), então o teste não é aleatório; a faixa de 45% a 55% tem 10 desvios-padrão de folga.
     */
    @Test
    void confrontosVizinhosDaMesmaFaseCoincidemEmCercaDeMetadeDosCasos() {
        for (int ordemFase = 1; ordemFase <= 3; ordemFase++) {
            assertThat(taxaDeCoincidencia(ordemFase, 1, ordemFase, 2)).as("fase %d, confrontos 1 e 2", ordemFase)
                    .isBetween(0.45, 0.55);
        }
    }

    @Test
    void todoParDeConfrontosVizinhosCoincideEmCercaDeMetade() {
        for (int ordemFase = 1; ordemFase <= 3; ordemFase++) {
            for (int posicao = 1; posicao <= 7; posicao++) {
                assertThat(taxaDeCoincidencia(ordemFase, posicao, ordemFase, posicao + 1))
                        .as("fase %d, confrontos %d e %d", ordemFase, posicao, posicao + 1).isBetween(0.45, 0.55);
            }
        }
    }

    @Test
    void mesmoConfrontoEmFasesDiferentesCoincideEmCercaDeMetade() {
        assertThat(taxaDeCoincidencia(1, 1, 2, 1)).isBetween(0.45, 0.55);
        assertThat(taxaDeCoincidencia(2, 1, 3, 1)).isBetween(0.45, 0.55);
    }

    @Test
    void osDoisLadosSaoSorteadosEmCercaDeMetadeDasVezes() {
        for (int ordemFase = 1; ordemFase <= 3; ordemFase++) {
            for (int posicao = 1; posicao <= 4; posicao++) {
                long favorecemA = 0;
                for (long seed = 0; seed < SEEDS; seed++) {
                    if (Sorteio.favoreceA(seed, ordemFase, posicao)) {
                        favorecemA++;
                    }
                }

                assertThat(favorecemA / (double) SEEDS).as("fase %d, confronto %d", ordemFase, posicao)
                        .isBetween(0.45, 0.55);
            }
        }
    }

    /** O detector da taxa funciona: a fórmula antiga (sem o SplitMix64) estoura a faixa, com mais de 99%. */
    @Test
    void aFormulaAntigaSemSplitMixSeriaPegaPeloTesteDeTaxa() {
        BiPredicate<Long, int[]> antiga = (seed, c) -> new Random(seed + 1_000_003L * c[0] + c[1])
                .nextBoolean();
        long iguais = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            if (antiga.test(seed, new int[] { 1, 1 }) == antiga.test(seed, new int[] { 1, 2 })) {
                iguais++;
            }
        }

        assertThat(iguais / (double) SEEDS).isGreaterThan(0.99);
    }

    /** Fração das seeds 0..9999 em que o sorteio dos dois confrontos dá o mesmo lado. */
    private static double taxaDeCoincidencia(int faseUm, int posicaoUm, int faseDois, int posicaoDois) {
        long iguais = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            if (Sorteio.favoreceA(seed, faseUm, posicaoUm) == Sorteio.favoreceA(seed, faseDois, posicaoDois)) {
                iguais++;
            }
        }
        return iguais / (double) SEEDS;
    }
}
