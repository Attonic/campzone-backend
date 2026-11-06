package br.com.codeacle.campzone.auth.api.response;

import br.com.codeacle.campzone.auth.domain.Papel;

/** Dados públicos do usuário autenticado. {@code organizacaoId} e {@code organizacaoNome} são nulos no admin. */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Papel papel,
        Long organizacaoId,
        String organizacaoNome) {
}
