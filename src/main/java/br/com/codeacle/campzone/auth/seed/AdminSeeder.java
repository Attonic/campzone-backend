package br.com.codeacle.campzone.auth.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.repository.UsuarioRepository;
import br.com.codeacle.campzone.common.config.SeedOrdem;

/**
 * Garante o admin da plataforma (ADR 0002) em todos os perfis. Idempotente: só cria se não existir usuário com o
 * e-mail e nunca sobrescreve a senha de um usuário existente.
 */
@Component
@Order(SeedOrdem.ADMIN)
@EnableConfigurationProperties(AdminProperties.class)
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminProperties propriedades;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminProperties propriedades, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.propriedades = propriedades;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (vazio(propriedades.email()) || vazio(propriedades.senha())) {
            log.warn("campzone.admin.email/senha não configurados: admin da plataforma não foi criado.");
            return;
        }
        String email = Usuario.normalizarEmail(propriedades.email());
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }
        try {
            usuarioRepository.saveAndFlush(
                    Usuario.adminPlataforma("Administrador", email, passwordEncoder.encode(propriedades.senha())));
            log.info("Admin da plataforma criado.");
        } catch (DataIntegrityViolationException e) {
            // Outra instância criou o admin ao mesmo tempo (unique em email): o resultado desejado já existe.
            log.debug("Admin da plataforma já criado por outra instância.");
        }
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
