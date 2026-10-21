package br.com.codeacle.campzone.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.codeacle.campzone.auth.api.request.CadastrarOrganizacaoRequest;
import br.com.codeacle.campzone.auth.api.request.LoginRequest;
import br.com.codeacle.campzone.auth.api.response.AutenticacaoResponse;
import br.com.codeacle.campzone.auth.api.response.UsuarioResponse;
import br.com.codeacle.campzone.auth.domain.Papel;
import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.exception.CredenciaisInvalidasException;
import br.com.codeacle.campzone.auth.exception.EmailJaCadastradoException;
import br.com.codeacle.campzone.auth.mapper.UsuarioMapper;
import br.com.codeacle.campzone.auth.repository.UsuarioRepository;
import br.com.codeacle.campzone.auth.service.TokenEmitido;
import br.com.codeacle.campzone.auth.service.TokenService;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.organizacao.service.OrganizacaoService;
import br.com.codeacle.campzone.plano.domain.Plano;

class AuthServiceImplTest {

    private static final Instant EXPIRA = Instant.parse("2026-10-21T20:00:00Z");

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final OrganizacaoService organizacaoService = mock(OrganizacaoService.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private AuthServiceImpl service;
    private Organizacao organizacao;

    @BeforeEach
    void preparar() {
        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash(" + i.getArgument(0) + ")");
        service = new AuthServiceImpl(usuarioRepository, organizacaoService, tokenService, passwordEncoder,
                new UsuarioMapper());
        organizacao = new Organizacao("Minha Org", new Plano("GRATIS", "Grátis", 1, 8));
        ReflectionTestUtils.setField(organizacao, "id", 5L);
        when(tokenService.emitir(any(Usuario.class))).thenReturn(new TokenEmitido("jwt-emitido", EXPIRA));
    }

    private Usuario usuarioSalvo(String email, String hash) {
        Usuario usuario = Usuario.organizador("Ana", email, hash, organizacao);
        ReflectionTestUtils.setField(usuario, "id", 9L);
        return usuario;
    }

    private CadastrarOrganizacaoRequest cadastro(String email) {
        return new CadastrarOrganizacaoRequest("Minha Org", "Ana", email, "senha-forte-1");
    }

    // --- cadastrar ---

    @Test
    void cadastrarCriaOrganizacaoEOrganizadorENormalizaOEmail() {
        when(usuarioRepository.existsByEmail("ana@x.com")).thenReturn(false);
        when(organizacaoService.criarNoPlanoGratis("Minha Org")).thenReturn(organizacao);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        AutenticacaoResponse resposta = service.cadastrar(cadastro("  Ana@X.com "));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo("ana@x.com");
        assertThat(salvo.getValue().getPapel()).isEqualTo(Papel.ORGANIZADOR);
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("hash(senha-forte-1)").doesNotContain("senha-forte-1 ");
        assertThat(salvo.getValue().getOrganizacao()).isSameAs(organizacao);
        assertThat(resposta.token()).isEqualTo("jwt-emitido");
        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.expiraEm()).isEqualTo(EXPIRA);
        assertThat(resposta.usuario().organizacaoId()).isEqualTo(5L);
        assertThat(resposta.usuario().organizacaoNome()).isEqualTo("Minha Org");
        assertThat(resposta.usuario().email()).isEqualTo("ana@x.com");
    }

    @Test
    void cadastrarComEmailExistenteLancaConflitoSemCriarOrganizacao() {
        when(usuarioRepository.existsByEmail("ana@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(cadastro("ANA@x.com ")))
                .isInstanceOf(EmailJaCadastradoException.class);
        verifyNoInteractions(organizacaoService);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void cadastrarTraduzCorridaDeEmailDuplicadoParaConflito() {
        when(usuarioRepository.existsByEmail("ana@x.com")).thenReturn(false);
        when(organizacaoService.criarNoPlanoGratis("Minha Org")).thenReturn(organizacao);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.cadastrar(cadastro("ana@x.com")))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    // --- login ---

    @Test
    void loginComCredenciaisCorretasDevolveToken() {
        Usuario usuario = usuarioSalvo("ana@x.com", "hash-guardado");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-forte-1", "hash-guardado")).thenReturn(true);

        AutenticacaoResponse resposta = service.login(new LoginRequest(" ANA@x.com", "senha-forte-1"));

        assertThat(resposta.token()).isEqualTo("jwt-emitido");
        assertThat(resposta.usuario().id()).isEqualTo(9L);
        assertThat(resposta.usuario().papel()).isEqualTo(Papel.ORGANIZADOR);
    }

    @Test
    void loginComSenhaErradaELoginComEmailInexistenteLancamAMesmaExcecao() {
        Usuario usuario = usuarioSalvo("ana@x.com", "hash-guardado");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("fantasma@x.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        Throwable senhaErrada = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.login(new LoginRequest("ana@x.com", "errada")));
        Throwable emailInexistente = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.login(new LoginRequest("fantasma@x.com", "qualquer")));

        assertThat(senhaErrada).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(emailInexistente).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(senhaErrada).hasMessage(emailInexistente.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void loginComEmailInexistenteAindaConfereUmHashParaIgualarOTempo() {
        when(usuarioRepository.findByEmail("fantasma@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("fantasma@x.com", "qualquer")))
                .isInstanceOf(CredenciaisInvalidasException.class);

        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void loginComEmailMalformadoVira401EmVezDeErroDeValidacaoDeDominio() {
        assertThatThrownBy(() -> service.login(new LoginRequest("sem-arroba", "qualquer")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(usuarioRepository, never()).findByEmail(any());
    }

    // --- buscarUsuario ---

    @Test
    void buscarUsuarioDevolveOsDadosDoToken() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuarioSalvo("ana@x.com", "h")));

        UsuarioResponse resposta = service.buscarUsuario(9L);

        assertThat(resposta.id()).isEqualTo(9L);
        assertThat(resposta.organizacaoNome()).isEqualTo("Minha Org");
    }

    @Test
    void buscarUsuarioDeAdminNaoTemOrganizacao() {
        Usuario admin = Usuario.adminPlataforma("Admin", "admin@x.com", "h");
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        UsuarioResponse resposta = service.buscarUsuario(1L);

        assertThat(resposta.papel()).isEqualTo(Papel.ADMIN_PLATAFORMA);
        assertThat(resposta.organizacaoId()).isNull();
        assertThat(resposta.organizacaoNome()).isNull();
    }

    @Test
    void buscarUsuarioInexistenteNaoAutentica() {
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarUsuario(404L)).isInstanceOf(BadCredentialsException.class);
    }
}
