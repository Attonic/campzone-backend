package br.com.codeacle.campzone.plano;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import br.com.codeacle.campzone.plano.domain.Plano;
import br.com.codeacle.campzone.plano.repository.PlanoRepository;
import br.com.codeacle.campzone.plano.seed.PlanoSeeder;
import br.com.codeacle.campzone.support.RepositorioTest;

@RepositorioTest
class PlanoSeederTest {

    @Autowired
    private PlanoRepository repository;

    @Autowired
    private TestEntityManager em;

    private void executarSeeder() {
        new PlanoSeeder(repository).run(new DefaultApplicationArguments());
        em.flush();
        em.clear();
    }

    private List<Plano> gratis() {
        return repository.findAll().stream().filter(p -> p.getCodigo().equals("GRATIS")).toList();
    }

    @Test
    void criaOPlanoGratisSeNaoExistir() {
        executarSeeder();

        assertThat(gratis()).singleElement().satisfies(plano -> {
            assertThat(plano.getNome()).isEqualTo("Grátis");
            assertThat(plano.getMaxCampeonatosAtivos()).isEqualTo(1);
            assertThat(plano.getMaxParticipantes()).isEqualTo(8);
        });
    }

    @Test
    void rodarDuasVezesNaoDuplica() {
        executarSeeder();
        executarSeeder();

        assertThat(gratis()).hasSize(1);
    }

    @Test
    void naoSobrescreveValoresAlteradosNoBanco() {
        em.getEntityManager().createNativeQuery(
                "insert into plano (codigo, nome, max_campeonatos_ativos, max_participantes) "
                        + "values ('GRATIS', 'Grátis customizado', 3, 16) on conflict (codigo) do update "
                        + "set nome = 'Grátis customizado', max_campeonatos_ativos = 3, max_participantes = 16")
                .executeUpdate();

        executarSeeder();

        assertThat(gratis()).singleElement().satisfies(plano -> {
            assertThat(plano.getNome()).isEqualTo("Grátis customizado");
            assertThat(plano.getMaxCampeonatosAtivos()).isEqualTo(3);
            assertThat(plano.getMaxParticipantes()).isEqualTo(16);
        });
    }
}
