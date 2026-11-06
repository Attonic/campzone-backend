package br.com.codeacle.campzone.auth.service;

import br.com.codeacle.campzone.auth.domain.Usuario;

/** Emite o JWT do usuário (HMAC-SHA256). */
public interface TokenService {

    /**
     * Claims: {@code sub} (id), {@code papel}, {@code org} (só organizador), {@code nome}, {@code iat}, {@code exp}.
     * A organização do organizador precisa estar acessível (pode ser um proxy, só o id é lido).
     */
    TokenEmitido emitir(Usuario usuario);
}
