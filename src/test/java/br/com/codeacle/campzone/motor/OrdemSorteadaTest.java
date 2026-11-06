package br.com.codeacle.campzone.motor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class OrdemSorteadaTest {

    @Test
    void ordemTrava() {
        // Com a seed 42, o shuffle do JDK sobre os ids [1, 2, 3, 4] dá [4, 2, 1, 3]
        assertThat(OrdemSorteada.de(ParticipantesDeTeste.comIds(1, 2, 3, 4), 42L)).containsExactly(4L, 2L, 1L, 3L);
    }

    @Test
    void mesmaSeedDaMesmaOrdemQualquerQueSejaAOrdemDeEntrada() {
        List<ParticipanteMotor> embaralhados = new ArrayList<>(ParticipantesDeTeste.com(9));
        Collections.shuffle(embaralhados, new Random(5));

        assertThat(OrdemSorteada.de(embaralhados, 7L)).isEqualTo(OrdemSorteada.de(ParticipantesDeTeste.com(9), 7L));
    }

    @Test
    void ordemContemTodosOsIdsUmaVezCada() {
        assertThat(OrdemSorteada.de(ParticipantesDeTeste.com(9), -3L))
                .containsExactlyInAnyOrderElementsOf(ParticipantesDeTeste.com(9).stream().map(ParticipanteMotor::id).toList());
    }

    @Test
    void listaVaziaDaOrdemVazia() {
        assertThat(OrdemSorteada.de(List.of(), 1L)).isEmpty();
    }
}
