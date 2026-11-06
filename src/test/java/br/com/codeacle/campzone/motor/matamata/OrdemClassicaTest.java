package br.com.codeacle.campzone.motor.matamata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OrdemClassicaTest {

    static Stream<Integer> tamanhos() {
        return Stream.of(2, 4, 8, 16, 32, 64);
    }

    @Test
    void ordemClassicaDasChavesPequenas() {
        assertThat(OrdemClassica.seeds(1)).containsExactly(1);
        assertThat(OrdemClassica.seeds(2)).containsExactly(1, 2);
        assertThat(OrdemClassica.seeds(4)).containsExactly(1, 4, 2, 3);
        assertThat(OrdemClassica.seeds(8)).containsExactly(1, 8, 4, 5, 2, 7, 3, 6);
        assertThat(OrdemClassica.seeds(16)).containsExactly(1, 16, 8, 9, 4, 13, 5, 12, 2, 15, 7, 10, 3, 14, 6, 11);
    }

    @ParameterizedTest(name = "chave de {0}")
    @MethodSource("tamanhos")
    void usaCadaSeedUmaVezECadaConfrontoSomaPMaisUm(int tamanho) {
        List<Integer> seeds = OrdemClassica.seeds(tamanho);

        assertThat(seeds).hasSize(tamanho).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, tamanho).boxed().toList());
        for (int i = 0; i < tamanho; i += 2) {
            assertThat(seeds.get(i) + seeds.get(i + 1)).as("confronto %d", i / 2 + 1).isEqualTo(tamanho + 1);
        }
    }

    /**
     * A propriedade que dá nome à ordem clássica: em blocos alinhados de 2^j posições há exatamente uma das melhores
     * {@code P / 2^j} seeds. Com j = log2(P) - 1 (metades), as seeds 1 e 2 ficam em metades opostas e só se
     * encontram na final; com blocos de um quarto, as quatro primeiras ficam em quartos diferentes, e assim por
     * diante.
     */
    @ParameterizedTest(name = "chave de {0}")
    @MethodSource("tamanhos")
    void asMelhoresSeedsFicamEmBlocosDiferentes(int tamanho) {
        List<Integer> seeds = OrdemClassica.seeds(tamanho);

        for (int tamanhoDoBloco = 2; tamanhoDoBloco <= tamanho; tamanhoDoBloco *= 2) {
            int bloco = tamanhoDoBloco;
            int quantidade = tamanho / bloco;
            // as 'quantidade' melhores seeds ocupam blocos distintos de tamanho 'bloco'
            long distintos = IntStream.rangeClosed(1, quantidade).map(seed -> seeds.indexOf(seed) / bloco).distinct()
                    .count();
            assertThat(distintos).as("chave %d, blocos de %d", tamanho, bloco).isEqualTo(quantidade);
        }
    }

    @Test
    void seedsUmEDoisFicamEmMetadesOpostas() {
        List<Integer> seeds = OrdemClassica.seeds(16);

        assertThat(seeds.indexOf(1)).isLessThan(8);
        assertThat(seeds.indexOf(2)).isGreaterThanOrEqualTo(8);
    }
}
