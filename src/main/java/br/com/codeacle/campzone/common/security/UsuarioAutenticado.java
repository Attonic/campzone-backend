package br.com.codeacle.campzone.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Dados do usuário lidos do JWT já validado. É o <b>único ponto</b> do sistema que lê {@code usuarioId} e
 * {@code organizacaoId} do token; os controllers os recebem por {@link OrganizacaoAtual} e {@link UsuarioAtual}.
 *
 * @param id            claim {@code sub}
 * @param organizacaoId claim {@code org}; {@code null} para o admin da plataforma
 */
public record UsuarioAutenticado(Long id, Long organizacaoId) {

    /** Extrai do {@link Authentication}; qualquer token que não seja um JWT válido do CampZone é acesso negado. */
    public static UsuarioAutenticado de(Authentication autenticacao) {
        if (!(autenticacao instanceof JwtAuthenticationToken token) || !autenticacao.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado por JWT.");
        }
        Jwt jwt = token.getToken();
        Long id = comoLong(jwt.getSubject());
        if (id == null) {
            // Token assinado mas sem identificação de usuário utilizável: não é um token nosso válido (401).
            throw new BadCredentialsException("Token sem identificação de usuário.");
        }
        return new UsuarioAutenticado(id, comoLong(jwt.getClaim(TokenClaims.ORGANIZACAO)));
    }

    /** O organizador sempre tem organização; o admin não tem e é barrado com 403 (AccessDeniedException). */
    public Long organizacaoIdObrigatorio() {
        if (organizacaoId == null) {
            throw new AccessDeniedException("O token não pertence a nenhuma organização.");
        }
        return organizacaoId;
    }

    private static Long comoLong(Object valor) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        if (valor instanceof String texto) {
            try {
                return Long.valueOf(texto);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
