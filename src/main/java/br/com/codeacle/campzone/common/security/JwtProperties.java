package br.com.codeacle.campzone.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração do JWT ({@code campzone.jwt.*}). O segredo assina com HMAC-SHA256 e precisa ter ao menos 32 bytes;
 * em prod vem de {@code JWT_SECRET}. Nunca é logado.
 *
 * @param segredo   chave HMAC (mínimo 32 bytes)
 * @param expiracao validade do token (padrão 8 horas); não há refresh token no MVP
 */
@ConfigurationProperties("campzone.jwt")
public record JwtProperties(String segredo, @DefaultValue("PT8H") Duration expiracao) {

    public static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    public JwtProperties {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalArgumentException("campzone.jwt.segredo é obrigatório e deve ter no mínimo "
                    + TAMANHO_MINIMO_SEGREDO_BYTES + " bytes.");
        }
        if (expiracao == null || expiracao.isZero() || expiracao.isNegative()) {
            throw new IllegalArgumentException("campzone.jwt.expiracao deve ser uma duração positiva.");
        }
    }

    /** Evita vazar o segredo em logs ou dumps do objeto. */
    @Override
    public String toString() {
        return "JwtProperties[segredo=***, expiracao=" + expiracao + "]";
    }
}
