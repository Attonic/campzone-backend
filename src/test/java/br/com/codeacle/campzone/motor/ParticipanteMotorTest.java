package br.com.codeacle.campzone.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParticipanteMotorTest {

    @Test
    void aceitaSeedNulaOuPreenchida() {
        assertThat(new ParticipanteMotor(1, "A", null).seed()).isNull();
        assertThat(new ParticipanteMotor(1, "A", 3).seed()).isEqualTo(3);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    void recusaNomeVazio(String nome) {
        assertThatThrownBy(() -> new ParticipanteMotor(5, nome, null))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("5");
    }

    @Test
    void excecoesDoMotorSaoNaoVerificadasComUmaRaizComum() {
        assertThat(new ConfigInvalidaException("x")).isInstanceOf(MotorException.class);
        assertThat(new ParticipantesInvalidosException("x")).isInstanceOf(MotorException.class);
        assertThat(new ResultadoInvalidoException("x")).isInstanceOf(MotorException.class);
        assertThat(new MotorException("x")).isInstanceOf(RuntimeException.class).hasMessage("x");
    }
}
