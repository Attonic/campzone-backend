package br.com.codeacle.campzone.auth.service;

import br.com.codeacle.campzone.auth.api.request.CadastrarOrganizacaoRequest;
import br.com.codeacle.campzone.auth.api.request.LoginRequest;
import br.com.codeacle.campzone.auth.api.response.AutenticacaoResponse;
import br.com.codeacle.campzone.auth.api.response.UsuarioResponse;

public interface AuthService {

    /** Cria a organização (plano GRATIS) e o organizador, e já devolve o token. 409 se o e-mail já existe. */
    AutenticacaoResponse cadastrar(CadastrarOrganizacaoRequest request);

    /** 401 com a mesma mensagem para e-mail inexistente e senha errada. */
    AutenticacaoResponse login(LoginRequest request);

    /** Dados do usuário do token. Se o usuário não existir mais, o token deixa de valer (401). */
    UsuarioResponse buscarUsuario(Long usuarioId);
}
