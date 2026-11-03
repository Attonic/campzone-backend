package br.com.codeacle.campzone.motor.matamata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

class SorteioTest {

    @Test
    void segueAFormulaDoAdendoDoAdr0005() {
        for (long seed : new long[] { 0, 1, 42, 20260, -7, Long.MAX_VALUE, Long.MIN_VALUE }) {
            for (int ordemFase = 1; ordemFase <= 6; ordemFase++) {
                for (int posicao = 1; posicao <= 8; posicao++) {
                    boolean esperado = new Random(seed + 1_000_003L * ordemFase + posicao).nextBoolean();

                    assertThat(Sorteio.favoreceA(seed, ordemFase, posicao)).as("seed %d, fase %d, confronto %d", seed,
                            ordemFase, posicao).isEqualTo(esperado);
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

    /** Trava a saída: o algoritmo de {@code Random(seed)} é fixado em contrato pelo JDK. */
    @Test
    void valoresTravados() {
        assertThat(Sorteio.favoreceA(42L, 1, 1)).isFalse();
        assertThat(Sorteio.favoreceA(42L, 2, 1)).isTrue();
        assertThat(Sorteio.favoreceA(20260L, 1, 1)).isFalse();
        assertThat(Sorteio.favoreceA(20260L, 3, 1)).isFalse();
        assertThat(Sorteio.favoreceA(987654321L, 4, 2)).isTrue();
    }
}
