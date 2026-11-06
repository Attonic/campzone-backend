package br.com.codeacle.campzone.auth.mapper;

import org.springframework.stereotype.Component;

import br.com.codeacle.campzone.auth.api.response.AutenticacaoResponse;
import br.com.codeacle.campzone.auth.api.response.UsuarioResponse;
import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.service.TokenEmitido;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;

@Component
public class UsuarioMapper {

    /** A organização precisa estar carregada (o admin não tem organização). */
    public UsuarioResponse toResponse(Usuario usuario) {
        Organizacao organizacao = usuario.getOrganizacao();
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                organizacao == null ? null : organizacao.getId(),
                organizacao == null ? null : organizacao.getNome());
    }

    public AutenticacaoResponse toAutenticacaoResponse(TokenEmitido token, Usuario usuario) {
        return new AutenticacaoResponse(token.token(), AutenticacaoResponse.TIPO_BEARER, token.expiraEm(),
                toResponse(usuario));
    }
}
