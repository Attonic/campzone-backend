package br.com.codeacle.campzone.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.codeacle.campzone.auth.domain.Papel;
import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.support.Cenario;
import br.com.codeacle.campzone.support.RepositorioTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;

@RepositorioTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void persisteOrganizadorEBuscaPorEmailComAOrganizacaoCarregada() {
        Organizacao org = Cenario.organizacao(em, "Org A");
        repository.saveAndFlush(Usuario.organizador("Ana", "ana@x.com", "hash", org));
        em.clear();

        Usuario achado = repository.findByEmail("ana@x.com").orElseThrow();

        PersistenceUnitUtil util = em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(achado, "organizacao")).isTrue();
        assertThat(achado.getPapel()).isEqualTo(Papel.ORGANIZADOR);
        assertThat(achado.getOrganizacao().getNome()).isEqualTo("Org A");
        assertThat(repository.existsByEmail("ana@x.com")).isTrue();
        assertThat(repository.existsByEmail("outra@x.com")).isFalse();
        assertThat(repository.findById(achado.getId())).isPresent();
    }

    @Test
    void adminPersisteSemOrganizacao() {
        repository.saveAndFlush(Usuario.adminPlataforma("Admin", "admin@x.com", "hash"));
        em.clear();

        Usuario admin = repository.findByEmail("admin@x.com").orElseThrow();

        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN_PLATAFORMA);
        assertThat(admin.getOrganizacao()).isNull();
    }

    @Test
    void papelEGravadoComoTextoSemCheck() {
        repository.saveAndFlush(Usuario.adminPlataforma("Admin", "admin@x.com", "hash"));
        EntityManager manager = em.getEntityManager();

        Object papel = manager.createNativeQuery("select papel from usuario where email = 'admin@x.com'")
                .getSingleResult();

        assertThat(papel).isEqualTo("ADMIN_PLATAFORMA");
    }

    @Test
    void emailEUnicoGlobalmente() {
        Organizacao a = Cenario.organizacao(em, "Org A");
        Organizacao b = Cenario.organizacao(em, "Org B");
        repository.saveAndFlush(Usuario.organizador("Ana", "dup@x.com", "hash", a));

        assertThatThrownBy(() -> repository.saveAndFlush(Usuario.organizador("Beto", "DUP@x.com ", "hash", b)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
