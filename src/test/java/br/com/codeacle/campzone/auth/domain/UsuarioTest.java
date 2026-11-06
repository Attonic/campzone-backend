package br.com.codeacle.campzone.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.common.domain.InvarianteDominioException;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.plano.domain.Plano;

class UsuarioTest {

    private final Organizacao org = new Organizacao("Org", new Plano("GRATIS", "Grátis", 1, 8));

    @Test
    void organizadorExigeOrganizacaoENormalizaOEmail() {
        Usuario usuario = Usuario.organizador("  Ana  ", "  Ana@Exemplo.COM ", "hash", org);

        assertThat(usuario.getNome()).isEqualTo("Ana");
        assertThat(usuario.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(usuario.getPapel()).isEqualTo(Papel.ORGANIZADOR);
        assertThat(usuario.getOrganizacao()).isSameAs(org);
        assertThat(usuario.getCriadoEm()).isNotNull();
        assertThatThrownBy(() -> Usuario.organizador("Ana", "ana@x.com", "hash", null))
                .isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void adminDaPlataformaNaoTemOrganizacao() {
        Usuario admin = Usuario.adminPlataforma("Admin", "admin@x.com", "hash");

        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN_PLATAFORMA);
        assertThat(admin.getOrganizacao()).isNull();
    }

    @Test
    void rejeitaDadosInvalidos() {
        assertThatThrownBy(() -> Usuario.adminPlataforma("", "a@x.com", "hash"))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Usuario.adminPlataforma("Admin", " ", "hash"))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Usuario.adminPlataforma("Admin", "sem-arroba", "hash"))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Usuario.adminPlataforma("Admin", "@x.com", "hash"))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Usuario.adminPlataforma("Admin", "a@x.com", ""))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Usuario.adminPlataforma("Admin", "a".repeat(250) + "@x.com", "hash"))
                .isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void autoridadeTemPrefixoRole() {
        assertThat(Papel.ADMIN_PLATAFORMA.autoridade()).isEqualTo("ROLE_ADMIN_PLATAFORMA");
        assertThat(Papel.ORGANIZADOR.autoridade()).isEqualTo("ROLE_ORGANIZADOR");
    }
}
