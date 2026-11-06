package br.com.codeacle.campzone.auth.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.codeacle.campzone.auth.domain.Papel;
import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.auth.repository.UsuarioRepository;
import br.com.codeacle.campzone.organizacao.repository.OrganizacaoRepository;
import br.com.codeacle.campzone.organizacao.service.impl.OrganizacaoServiceImpl;
import br.com.codeacle.campzone.plano.repository.PlanoRepository;
import br.com.codeacle.campzone.plano.seed.PlanoSeeder;
import br.com.codeacle.campzone.support.RepositorioTest;

@ExtendWith(OutputCaptureExtension.class)
@RepositorioTest
class SeedersTest {

    private static final String EMAIL_ADMIN = "admin-seed@campzone.dev";
    private static final String EMAIL_ORGANIZADOR = "organizador-seed@campzone.dev";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private OrganizacaoRepository organizacaoRepository;

    @Autowired
    private PlanoRepository planoRepository;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private TestEntityManager em;

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private void rodarAdmin(String email, String senha) {
        new AdminSeeder(new AdminProperties(email, senha), usuarioRepository, encoder)
                .run(new DefaultApplicationArguments());
        em.flush();
        em.clear();
    }

    private void rodarDev(String senha) {
        new PlanoSeeder(planoRepository).run(new DefaultApplicationArguments());
        new DevDadosSeeder(EMAIL_ORGANIZADOR, senha, usuarioRepository,
                new OrganizacaoServiceImpl(organizacaoRepository, planoRepository), encoder, transacao)
                .run(new DefaultApplicationArguments());
        em.flush();
        em.clear();
    }

    private long contar(String email) {
        return ((Number) em.getEntityManager()
                .createNativeQuery("select count(*) from usuario where email = :email")
                .setParameter("email", email).getSingleResult()).longValue();
    }

    @Test
    void adminSeederCriaOAdminSemOrganizacaoEComSenhaEmHash() {
        rodarAdmin(EMAIL_ADMIN, "admin12345");

        Usuario admin = usuarioRepository.findByEmail(EMAIL_ADMIN).orElseThrow();
        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN_PLATAFORMA);
        assertThat(admin.getOrganizacao()).isNull();
        assertThat(admin.getSenhaHash()).isNotEqualTo("admin12345").startsWith("{bcrypt}");
        assertThat(encoder.matches("admin12345", admin.getSenhaHash())).isTrue();
    }

    @Test
    void adminSeederRodadoDuasVezesNaoDuplicaENaoSobrescreveASenha() {
        rodarAdmin(EMAIL_ADMIN, "admin12345");
        String hashOriginal = usuarioRepository.findByEmail(EMAIL_ADMIN).orElseThrow().getSenhaHash();
        em.clear();

        rodarAdmin(EMAIL_ADMIN, "outra-senha-qualquer");

        assertThat(contar(EMAIL_ADMIN)).isEqualTo(1);
        assertThat(usuarioRepository.findByEmail(EMAIL_ADMIN).orElseThrow().getSenhaHash()).isEqualTo(hashOriginal);
    }

    @Test
    void adminSeederNaoSobrescreveSenhaDeUsuarioExistenteComOMesmoEmail() {
        em.persist(Usuario.adminPlataforma("Antigo", EMAIL_ADMIN, "hash-antigo"));
        em.flush();
        em.clear();

        rodarAdmin(EMAIL_ADMIN, "admin12345");

        assertThat(usuarioRepository.findByEmail(EMAIL_ADMIN).orElseThrow().getSenhaHash()).isEqualTo("hash-antigo");
    }

    @Test
    void adminSeederAvisaQuandoOEmailDoAdminJaPertenceAUmOrganizador(CapturedOutput saida) {
        var organizacao = em.persist(new br.com.codeacle.campzone.organizacao.domain.Organizacao("Org",
                planoRepository.findByCodigo("GRATIS").orElseGet(
                        () -> em.persist(new br.com.codeacle.campzone.plano.domain.Plano("GRATIS", "Grátis", 1, 8)))));
        em.persist(Usuario.organizador("Intruso", EMAIL_ADMIN, "hash-intruso", organizacao));
        em.flush();
        em.clear();

        rodarAdmin(EMAIL_ADMIN, "admin12345");

        assertThat(saida.getAll()).contains("o admin da plataforma não foi criado");
        assertThat(usuarioRepository.findByEmail(EMAIL_ADMIN).orElseThrow().getPapel()).isEqualTo(Papel.ORGANIZADOR);
    }

    @Test
    void adminSeederNaoAvisaQuandoOAdminJaExiste(CapturedOutput saida) {
        rodarAdmin(EMAIL_ADMIN, "admin12345");
        rodarAdmin(EMAIL_ADMIN, "admin12345");

        assertThat(saida.getAll()).doesNotContain("o admin da plataforma não foi criado");
    }

    @Test
    void adminSeederSemCredenciaisConfiguradasNaoCriaNada() {
        rodarAdmin(" ", "admin12345");
        rodarAdmin(EMAIL_ADMIN, null);

        assertThat(contar(EMAIL_ADMIN)).isZero();
    }

    @Test
    void devSeederCriaOrganizacaoDemoNoPlanoGratisEOrganizador() {
        rodarDev("organizador123");

        Usuario organizador = usuarioRepository.findByEmail(EMAIL_ORGANIZADOR).orElseThrow();
        assertThat(organizador.getPapel()).isEqualTo(Papel.ORGANIZADOR);
        assertThat(organizador.getOrganizacao().getNome()).isEqualTo("Organização Demo");
        assertThat(organizador.getOrganizacao().getPlano().getCodigo()).isEqualTo("GRATIS");
        assertThat(encoder.matches("organizador123", organizador.getSenhaHash())).isTrue();
    }

    @Test
    void devSeederRodadoDuasVezesNaoDuplicaOrganizacaoNemUsuario() {
        rodarDev("organizador123");
        long organizacoesAntes = ((Number) em.getEntityManager()
                .createNativeQuery("select count(*) from organizacao").getSingleResult()).longValue();

        rodarDev("organizador123");

        assertThat(contar(EMAIL_ORGANIZADOR)).isEqualTo(1);
        assertThat(((Number) em.getEntityManager().createNativeQuery("select count(*) from organizacao")
                .getSingleResult()).longValue()).isEqualTo(organizacoesAntes);
    }
}
