package br.com.codeacle.campzone.common.security;

import java.nio.charset.StandardCharsets;

import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Encoder e decoder do JWT, ambos HMAC-SHA256 com o mesmo segredo ({@code campzone.jwt.segredo}). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static SecretKey chave(JwtProperties propriedades) {
        return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties propriedades) {
        return NimbusJwtEncoder.withSecretKey(chave(propriedades)).algorithm(MacAlgorithm.HS256).build();
    }

    /**
     * Só aceita HS256 e exige o claim {@code exp}: o validador padrão trata token sem expiração como válido, e um
     * token eterno não pode existir. Além do {@code exp}, valida {@code nbf} (padrão do Spring).
     */
    @Bean
    JwtDecoder jwtDecoder(JwtProperties propriedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(propriedades))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtClaimValidator<Object>(JwtClaimNames.EXP, Objects::nonNull)));
        return decoder;
    }
}
