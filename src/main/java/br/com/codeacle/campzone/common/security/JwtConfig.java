package br.com.codeacle.campzone.common.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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

    @Bean
    JwtDecoder jwtDecoder(JwtProperties propriedades) {
        return NimbusJwtDecoder.withSecretKey(chave(propriedades)).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
