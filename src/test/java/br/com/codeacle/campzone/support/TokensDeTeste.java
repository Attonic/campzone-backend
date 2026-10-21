package br.com.codeacle.campzone.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Fábricas de JWT e de autenticações JWT para os testes de segurança. */
public final class TokensDeTeste {

    /** Segredo usado pelos web tests (mínimo de 32 bytes). */
    public static final String SEGREDO = "segredo-de-teste-campzone-0123456789-abcdef";

    public static final String OUTRO_SEGREDO = "outro-segredo-de-teste-campzone-9876543210";

    private TokensDeTeste() {
    }

    /** JWT assinado com HS256 e os claims informados; {@code papel}/{@code org} são opcionais. */
    public static String token(String segredo, Instant emitidoEm, Instant expiraEm, long usuarioId, String papel,
            Long organizacaoId) {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("papel", papel);
        extras.put("nome", "Fulano");
        if (organizacaoId != null) {
            extras.put("org", organizacaoId);
        }
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuarioId))
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .claims(mapa -> mapa.putAll(extras))
                .build();
        var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256")).algorithm(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public static String tokenValido(String papel, Long organizacaoId) {
        Instant agora = Instant.now();
        return token(SEGREDO, agora, agora.plus(Duration.ofHours(1)), 1L, papel, organizacaoId);
    }

    public static String tokenExpirado() {
        Instant agora = Instant.now();
        return token(SEGREDO, agora.minus(Duration.ofHours(3)), agora.minus(Duration.ofHours(2)), 1L, "ORGANIZADOR",
                1L);
    }

    public static String tokenAssinadoComOutroSegredo() {
        Instant agora = Instant.now();
        return token(OUTRO_SEGREDO, agora, agora.plus(Duration.ofHours(1)), 1L, "ORGANIZADOR", 1L);
    }

    /** Autenticação JWT de organizador, sem passar pelo decoder (para web tests que mockam o service). */
    public static RequestPostProcessor organizador(long usuarioId, long organizacaoId) {
        return jwt().jwt(j -> j.subject(String.valueOf(usuarioId)).claim("papel", "ORGANIZADOR")
                .claim("org", organizacaoId))
                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"));
    }

    /** Autenticação JWT de admin da plataforma: sem claim {@code org}. */
    public static RequestPostProcessor admin(long usuarioId) {
        return jwt().jwt(j -> j.subject(String.valueOf(usuarioId)).claim("papel", "ADMIN_PLATAFORMA"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_PLATAFORMA"));
    }
}
