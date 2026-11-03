package br.com.codeacle.campzone.motor.matamata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.motor.ResultadoInvalidoException;

class ResultadoMataMataTest {

    @Test
    void guardaAChaveOPlacarEOsDadosDeDesempate() {
        ResultadoMataMata r = new ResultadoMataMata(2, 3, 1, 1, 1, 0, 0, 4, 3);

        assertThat(r.ordemFase()).isEqualTo(2);
        assertThat(r.posicao()).isEqualTo(3);
        assertThat(r.numeroJogo()).isEqualTo(1);
        assertThat(r.placarMandante()).isEqualTo(1);
        assertThat(r.placarVisitante()).isEqualTo(1);
        assertThat(r.prorrogacaoMandante()).isZero();
        assertThat(r.prorrogacaoVisitante()).isZero();
        assertThat(r.penaltisMandante()).isEqualTo(4);
        assertThat(r.penaltisVisitante()).isEqualTo(3);
        assertThat(r.temProrrogacao()).isTrue();
        assertThat(r.temPenaltis()).isTrue();
    }

    @Test
    void prorrogacaoEPenaltisSaoOpcionais() {
        ResultadoMataMata r = new ResultadoMataMata(1, 1, 1, 2, 0, null, null, null, null);

        assertThat(r.temProrrogacao()).isFalse();
        assertThat(r.temPenaltis()).isFalse();
    }

    @Test
    void aceitaSoProrrogacaoOuSoPenaltis() {
        assertThatCode(() -> new ResultadoMataMata(1, 1, 1, 1, 1, 1, 0, null, null)).doesNotThrowAnyException();
        assertThatCode(() -> new ResultadoMataMata(1, 1, 1, 1, 1, null, null, 5, 4)).doesNotThrowAnyException();
    }

    @Test
    void recusaPlacarNegativo() {
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, -1, 0, null, null, null, null))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("negativo");
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 0, -2, null, null, null, null))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void recusaChaveNegativa() {
        assertThatThrownBy(() -> new ResultadoMataMata(-1, 1, 1, 0, 0, null, null, null, null))
                .isInstanceOf(ResultadoInvalidoException.class);
        assertThatThrownBy(() -> new ResultadoMataMata(1, -1, 1, 0, 0, null, null, null, null))
                .isInstanceOf(ResultadoInvalidoException.class);
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, -1, 0, 0, null, null, null, null))
                .isInstanceOf(ResultadoInvalidoException.class);
    }

    @Test
    void recusaProrrogacaoNegativaOuIncompleta() {
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 1, 1, -1, 0, null, null))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("prorrogação");
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 1, 1, 0, -1, null, null))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("prorrogação");
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 1, 1, 1, null, null, null))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("prorrogação");
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 1, 1, null, 1, null, null))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("prorrogação");
    }

    @Test
    void recusaPenaltisNegativosOuIncompletos() {
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 1, 1, null, null, -1, 3))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("pênaltis");
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 1, 1, null, null, 3, -1))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("pênaltis");
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 1, 1, null, null, 3, null))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("pênaltis");
        assertThatThrownBy(() -> new ResultadoMataMata(1, 1, 1, 1, 1, null, null, null, 3))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("pênaltis");
    }
}
