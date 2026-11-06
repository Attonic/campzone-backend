package br.com.codeacle.campzone.motor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ValidadorParticipantesTest {

    @Test
    void aceitaDoisOuMaisComIdsUnicos() {
        assertThatCode(() -> ValidadorParticipantes.validar(ParticipantesDeTeste.com(2))).doesNotThrowAnyException();
        assertThatCode(() -> ValidadorParticipantes.validar(ParticipantesDeTeste.com(16))).doesNotThrowAnyException();
    }

    @Test
    void recusaListaNula() {
        assertThatThrownBy(() -> ValidadorParticipantes.validar(null))
                .isInstanceOf(ParticipantesInvalidosException.class);
    }

    @Test
    void recusaItemNulo() {
        List<ParticipanteMotor> comNulo = Arrays.asList(new ParticipanteMotor(1, "A", null), null);
        assertThatThrownBy(() -> ValidadorParticipantes.validar(comNulo))
                .isInstanceOf(ParticipantesInvalidosException.class);
    }

    @Test
    void recusaMenosDeDois() {
        assertThatThrownBy(() -> ValidadorParticipantes.validar(List.of()))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("pelo menos 2");
        assertThatThrownBy(() -> ValidadorParticipantes.validar(ParticipantesDeTeste.com(1)))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("pelo menos 2");
    }

    @Test
    void recusaIdRepetido() {
        assertThatThrownBy(() -> ValidadorParticipantes.validar(ParticipantesDeTeste.comIds(1, 2, 1)))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("repetido")
                .hasMessageContaining("1");
    }
}
