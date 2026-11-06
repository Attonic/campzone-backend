package br.com.codeacle.campzone.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.service.TokenEmitido;
import br.com.codeacle.campzone.common.security.JwtProperties;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.plano.domain.Plano;
import br.com.codeacle.campzone.support.TokensDeTeste;

class TokenServiceImplTest {

    // O decoder valida a expiração com o relógio real, então o "agora" do teste é o agora de verdade.
    private static final Instant AGORA = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
            .plusMillis(789);

    private final SecretKey chave = new SecretKeySpec(TokensDeTeste.SEGREDO.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256");
    private final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave)
            .macAlgorithm(MacAlgorithm.HS256).build();
    private final TokenServiceImpl service = new TokenServiceImpl(
            NimbusJwtEncoder.withSecretKey(chave).algorithm(MacAlgorithm.HS256).build(),
            new JwtProperties(TokensDeTeste.SEGREDO, Duration.ofHours(8)),
            Clock.fixed(AGORA, ZoneOffset.UTC));

    private Usuario organizador() {
        Organizacao org = new Organizacao("Org", new Plano("GRATIS", "Grátis", 1, 8));
        ReflectionTestUtils.setField(org, "id", 7L);
        Usuario usuario = Usuario.organizador("Ana", "ana@x.com", "hash", org);
        ReflectionTestUtils.setField(usuario, "id", 42L);
        return usuario;
    }

    @Test
    void tokenDoOrganizadorTemTodosOsClaims() {
        TokenEmitido emitido = service.emitir(organizador());

        Jwt jwt = decoder.decode(emitido.token());
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.<String>getClaim("papel")).isEqualTo("ORGANIZADOR");
        assertThat(jwt.<Long>getClaim("org")).isEqualTo(7L);
        assertThat(jwt.<String>getClaim("nome")).isEqualTo("Ana");
        Instant emitidoEm = AGORA.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        assertThat(jwt.getIssuedAt()).isEqualTo(emitidoEm);
        assertThat(jwt.getExpiresAt()).isEqualTo(emitidoEm.plus(Duration.ofHours(8)));
        assertThat(emitido.expiraEm()).isEqualTo(jwt.getExpiresAt());
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void tokenDoAdminNaoTemClaimDeOrganizacao() {
        Usuario admin = Usuario.adminPlataforma("Admin", "admin@x.com", "hash");
        ReflectionTestUtils.setField(admin, "id", 1L);

        Jwt jwt = decoder.decode(service.emitir(admin).token());

        assertThat(jwt.<String>getClaim("papel")).isEqualTo("ADMIN_PLATAFORMA");
        assertThat(jwt.getClaims()).doesNotContainKey("org");
    }

    @Test
    void toStringDoTokenEmitidoNaoRevelaOToken() {
        TokenEmitido emitido = service.emitir(organizador());

        assertThat(emitido.toString()).doesNotContain(emitido.token());
    }
}
