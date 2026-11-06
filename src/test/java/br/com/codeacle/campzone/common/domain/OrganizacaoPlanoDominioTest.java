package br.com.codeacle.campzone.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.plano.domain.Plano;

class OrganizacaoPlanoDominioTest {

    @Test
    void planoExigeCodigoNomeELimitesNaoNegativos() {
        Plano plano = new Plano(" GRATIS ", "Grátis", 1, 8);

        assertThat(plano.getCodigo()).isEqualTo("GRATIS");
        assertThat(plano.getMaxCampeonatosAtivos()).isEqualTo(1);
        assertThat(plano.getMaxParticipantes()).isEqualTo(8);
        assertThatThrownBy(() -> new Plano("", "Grátis", 1, 8)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Plano("X", " ", 1, 8)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Plano("X", "X", -1, 8)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Plano("X", "X", 1, -1)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void organizacaoExigeNomeEPlano() {
        Plano plano = new Plano("GRATIS", "Grátis", 1, 8);
        Organizacao org = new Organizacao(" Minha Org ", plano);

        assertThat(org.getNome()).isEqualTo("Minha Org");
        assertThat(org.getPlano()).isSameAs(plano);
        assertThat(org.getCriadoEm()).isNotNull();
        assertThatThrownBy(() -> new Organizacao("", plano)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Organizacao("Org", null)).isInstanceOf(InvarianteDominioException.class);

        org.renomear("Nova");
        assertThat(org.getNome()).isEqualTo("Nova");
    }
}
