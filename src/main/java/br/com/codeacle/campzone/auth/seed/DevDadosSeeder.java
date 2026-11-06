package br.com.codeacle.campzone.auth.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.repository.UsuarioRepository;
import br.com.codeacle.campzone.common.config.SeedOrdem;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.organizacao.service.OrganizacaoService;

/**
 * Só em {@code dev}: cria a organização demo (plano {@code GRATIS}) e o organizador de teste, para entrar sem
 * cadastro. As credenciais vêm do {@code application-dev.yml} (não são segredo). Idempotente.
 */
@Component
@Profile("dev")
@Order(SeedOrdem.DEV_DADOS)
public class DevDadosSeeder implements ApplicationRunner {

    static final String NOME_ORGANIZACAO = "Organização Demo";

    private static final Logger log = LoggerFactory.getLogger(DevDadosSeeder.class);

    private final String emailOrganizador;
    private final String senhaOrganizador;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoService organizacaoService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transacao;

    public DevDadosSeeder(@Value("${campzone.dev.organizador.email}") String emailOrganizador,
            @Value("${campzone.dev.organizador.senha}") String senhaOrganizador,
            UsuarioRepository usuarioRepository, OrganizacaoService organizacaoService,
            PasswordEncoder passwordEncoder, TransactionTemplate transacao) {
        this.emailOrganizador = emailOrganizador;
        this.senhaOrganizador = senhaOrganizador;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoService = organizacaoService;
        this.passwordEncoder = passwordEncoder;
        this.transacao = transacao;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = Usuario.normalizarEmail(emailOrganizador);
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }
        try {
            // Organização e organizador na mesma transação: nunca sobra organização sem dono.
            transacao.executeWithoutResult(status -> {
                Organizacao organizacao = organizacaoService.criarNoPlanoGratis(NOME_ORGANIZACAO);
                usuarioRepository.saveAndFlush(Usuario.organizador("Organizador Demo", email,
                        passwordEncoder.encode(senhaOrganizador), organizacao));
            });
            log.info("Organização demo e organizador de desenvolvimento criados.");
        } catch (DataIntegrityViolationException e) {
            log.debug("Dados de desenvolvimento já criados por outra instância.");
        }
    }
}
