package br.com.codeacle.campzone.auth.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.codeacle.campzone.auth.api.request.CadastrarOrganizacaoRequest;
import br.com.codeacle.campzone.auth.api.request.LoginRequest;
import br.com.codeacle.campzone.auth.api.response.AutenticacaoResponse;
import br.com.codeacle.campzone.auth.api.response.UsuarioResponse;
import br.com.codeacle.campzone.auth.service.AuthService;
import br.com.codeacle.campzone.common.security.UsuarioAtual;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/public/auth/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public AutenticacaoResponse cadastrar(@Valid @RequestBody CadastrarOrganizacaoRequest request) {
        return authService.cadastrar(request);
    }

    @PostMapping("/public/auth/login")
    public AutenticacaoResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/auth/me")
    public UsuarioResponse me(@UsuarioAtual Long usuarioId) {
        return authService.buscarUsuario(usuarioId);
    }
}
