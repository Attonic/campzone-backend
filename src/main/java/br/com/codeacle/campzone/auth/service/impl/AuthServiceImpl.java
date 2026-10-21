package br.com.codeacle.campzone.auth.service.impl;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.codeacle.campzone.auth.api.request.CadastrarOrganizacaoRequest;
import br.com.codeacle.campzone.auth.api.request.LoginRequest;
import br.com.codeacle.campzone.auth.api.response.AutenticacaoResponse;
import br.com.codeacle.campzone.auth.api.response.UsuarioResponse;
import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.exception.CredenciaisInvalidasException;
import br.com.codeacle.campzone.auth.exception.EmailJaCadastradoException;
import br.com.codeacle.campzone.auth.exception.TokenDeUsuarioInexistenteException;
import br.com.codeacle.campzone.auth.mapper.UsuarioMapper;
import br.com.codeacle.campzone.auth.repository.UsuarioRepository;
import br.com.codeacle.campzone.auth.service.AuthService;
import br.com.codeacle.campzone.auth.service.TokenEmitido;
import br.com.codeacle.campzone.auth.service.TokenService;
import br.com.codeacle.campzone.common.domain.InvarianteDominioException;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.organizacao.service.OrganizacaoService;

@Service
public class AuthServiceImpl implements AuthService {

    /** Limite do BCrypt, em bytes UTF-8. */
    static final int SENHA_MAXIMO_BYTES = 72;

    private static final String SENHA_DESCARTAVEL = "senha-descartavel-para-igualar-o-tempo";

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoService organizacaoService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;

    /** Hash de uma senha descartável: o login de e-mail inexistente gasta o mesmo tempo que o de senha errada. */
    private final String hashParaIgualarTempo;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, OrganizacaoService organizacaoService,
            TokenService tokenService, PasswordEncoder passwordEncoder, UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.organizacaoService = organizacaoService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.hashParaIgualarTempo = passwordEncoder.encode(SENHA_DESCARTAVEL);
    }

    @Override
    @Transactional
    public AutenticacaoResponse cadastrar(CadastrarOrganizacaoRequest request) {
        String email = Usuario.normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }

        Organizacao organizacao = organizacaoService.criarNoPlanoGratis(request.nomeOrganizacao());
        Usuario organizador = Usuario.organizador(request.nome(), email, passwordEncoder.encode(request.senha()),
                organizacao);
        try {
            usuarioRepository.saveAndFlush(organizador);
        } catch (DataIntegrityViolationException e) {
            // Outro cadastro com o mesmo e-mail venceu a corrida (unique uk_usuario_email). A transação reverte
            // também a organização criada acima.
            throw new EmailJaCadastradoException();
        }

        log.info("Organização {} cadastrada com o organizador {}.", organizacao.getId(), organizador.getId());
        return resposta(organizador);
    }

    @Override
    @Transactional(readOnly = true)
    public AutenticacaoResponse login(LoginRequest request) {
        Usuario usuario = buscarPorEmail(request.email());
        if (request.senha().getBytes(StandardCharsets.UTF_8).length > SENHA_MAXIMO_BYTES) {
            // Nenhuma senha cadastrada passa do limite do BCrypt, que lançaria erro: recusa com o 401 genérico
            // (sem mensagem própria, para não revelar a regra) gastando o mesmo tempo de uma conferência.
            passwordEncoder.matches(SENHA_DESCARTAVEL, hashParaIgualarTempo);
            throw new CredenciaisInvalidasException();
        }
        String hash = usuario != null ? usuario.getSenhaHash() : hashParaIgualarTempo;
        boolean senhaConfere = passwordEncoder.matches(request.senha(), hash);
        if (usuario == null || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }
        return resposta(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarUsuario(Long usuarioId) {
        // Token válido de usuário que não existe mais: não autenticado (401).
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(TokenDeUsuarioInexistenteException::new);
        return mapper.toResponse(usuario);
    }

    private Usuario buscarPorEmail(String emailInformado) {
        try {
            return usuarioRepository.findByEmail(Usuario.normalizarEmail(emailInformado)).orElse(null);
        } catch (InvarianteDominioException e) {
            // E-mail malformado ou grande demais: trata como inexistente, sem revelar o motivo.
            return null;
        }
    }

    private AutenticacaoResponse resposta(Usuario usuario) {
        TokenEmitido token = tokenService.emitir(usuario);
        return mapper.toAutenticacaoResponse(token, usuario);
    }
}
