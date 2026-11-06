package br.com.codeacle.campzone.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ResultadoJogoTest {

    @Test
    void aceitaPlacarZeradoEEmpate() {
        ResultadoJogo resultado = new ResultadoJogo(1, 2, 0, 0);
        assertThat(resultado.placarMandante()).isZero();
        assertThat(resultado.placarVisitante()).isZero();
    }

    @Test
    void recusaPlacarNegativoDoMandante() {
        assertThatThrownBy(() -> new ResultadoJogo(1, 2, -1, 0))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void recusaPlacarNegativoDoVisitante() {
        assertThatThrownBy(() -> new ResultadoJogo(1, 2, 0, -3))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void recusaMandanteIgualAoVisitante() {
        assertThatThrownBy(() -> new ResultadoJogo(4, 4, 1, 0))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("contra si mesmo");
    }
}
