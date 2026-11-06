package br.com.codeacle.campzone.auth.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.service.TokenEmitido;
import br.com.codeacle.campzone.auth.service.TokenService;
import br.com.codeacle.campzone.common.security.JwtProperties;
import br.com.codeacle.campzone.common.security.TokenClaims;

@Service
public class TokenServiceImpl implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties propriedades;
    private final Clock clock;

    public TokenServiceImpl(JwtEncoder jwtEncoder, JwtProperties propriedades, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.propriedades = propriedades;
        this.clock = clock;
    }

    @Override
    public TokenEmitido emitir(Usuario usuario) {
        // Os claims de tempo do JWT têm precisão de segundos.
        Instant emitidoEm = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = emitidoEm.plus(propriedades.expiracao());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .claim(TokenClaims.PAPEL, usuario.getPapel().name())
                .claim(TokenClaims.NOME, usuario.getNome());
        if (usuario.getOrganizacao() != null) {
            claims.claim(TokenClaims.ORGANIZACAO, usuario.getOrganizacao().getId());
        }

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims.build())).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }
}
