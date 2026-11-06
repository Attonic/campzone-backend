package br.com.codeacle.campzone.auth.service;

import java.time.Instant;

/** JWT emitido e o instante em que expira. */
public record TokenEmitido(String token, Instant expiraEm) {

    @Override
    public String toString() {
        return "TokenEmitido[token=***, expiraEm=" + expiraEm + "]";
    }
}
