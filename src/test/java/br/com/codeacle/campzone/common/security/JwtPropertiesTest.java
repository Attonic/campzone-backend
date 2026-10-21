package br.com.codeacle.campzone.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private static final String SEGREDO_32 = "0123456789abcdef0123456789abcdef";

    @Test
    void aceitaSegredoDe32BytesOuMais() {
        JwtProperties propriedades = new JwtProperties(SEGREDO_32, Duration.ofHours(8));

        assertThat(propriedades.segredo()).isEqualTo(SEGREDO_32);
        assertThat(propriedades.expiracao()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void rejeitaSegredoAusenteOuCurto() {
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("campzone.jwt.segredo");
        assertThatThrownBy(() -> new JwtProperties("curto", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new JwtProperties(SEGREDO_32.substring(1), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaExpiracaoNaoPositiva() {
        assertThatThrownBy(() -> new JwtProperties(SEGREDO_32, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(SEGREDO_32, Duration.ofHours(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(SEGREDO_32, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringNaoRevelaOSegredo() {
        assertThat(new JwtProperties(SEGREDO_32, Duration.ofHours(1)).toString()).doesNotContain(SEGREDO_32);
    }
}
