package br.com.codeacle.campzone.auth.api.response;

import java.time.Instant;

/** Resultado de um cadastro ou login: o JWT, o instante em que expira e os dados do usuário. */
public record AutenticacaoResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResponse usuario) {

    public static final String TIPO_BEARER = "Bearer";

    /** Evita o token no log caso o record seja impresso. */
    @Override
    public String toString() {
        return "AutenticacaoResponse[token=***, tipo=" + tipo + ", expiraEm=" + expiraEm + ", usuario=" + usuario + "]";
    }
}
