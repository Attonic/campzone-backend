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

    /** Token de organizador válido, mas com o {@code sub} informado (para testar subs inválidos). */
    public static String tokenComSub(String segredo, String sub, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(sub).issuedAt(Instant.now()).expiresAt(expiraEm)
                .claim("papel", "ORGANIZADOR").claim("org", 1L).build();
        return codificar(segredo, claims);
    }

    private static String codificar(String segredo, JwtClaimsSet claims) {
        var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256")).algorithm(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    /** HS256 com a assinatura correta, mas sem o claim {@code exp}. */
    public static String tokenSemExp() {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").issuedAt(Instant.now())
                .claim("papel", "ORGANIZADOR").claim("org", 1L).build();
        return codificar(SEGREDO, claims);
    }

    /**
     * JWT montado à mão: cabeçalho e payload em JSON e assinatura HMAC com o algoritmo JCA informado (ou lixo, se
     * {@code algoritmoJca} for nulo). Serve para testar algoritmos que o encoder do Spring não emite.
     */
    public static String manual(String cabecalhoJson, String algoritmoJca, String segredo) {
        var base64 = java.util.Base64.getUrlEncoder().withoutPadding();
        long exp = Instant.now().plusSeconds(3600).getEpochSecond();
        String payload = "{\"sub\":\"1\",\"papel\":\"ORGANIZADOR\",\"org\":1,\"exp\":" + exp + "}";
        String conteudo = base64.encodeToString(cabecalhoJson.getBytes(StandardCharsets.UTF_8)) + "."
                + base64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        byte[] assinatura = new byte[64];
        if (algoritmoJca != null) {
            try {
                var mac = javax.crypto.Mac.getInstance(algoritmoJca);
                mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), algoritmoJca));
                assinatura = mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8));
            } catch (java.security.GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }
        return conteudo + "." + base64.encodeToString(assinatura);
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
