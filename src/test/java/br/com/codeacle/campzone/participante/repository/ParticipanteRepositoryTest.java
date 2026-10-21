package br.com.codeacle.campzone.participante.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.participante.domain.Participante;
import br.com.codeacle.campzone.support.Cenario;
import br.com.codeacle.campzone.support.RepositorioTest;

@RepositorioTest
class ParticipanteRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ParticipanteRepository repository;

    @Test
    void persisteELeParticipanteComSeed() {
        Organizacao org = Cenario.organizacao(em, "Org");
        Campeonato campeonato = Cenario.pontosCorridos(em, org);
        Participante novo = repository.save(new Participante(campeonato, " Time A ", 1));
        em.flush();
        em.clear();

        Participante lido = repository.findByIdAndOrganizacaoId(novo.getId(), org.getId()).orElseThrow();

        assertThat(lido.getNome()).isEqualTo("Time A");
        assertThat(lido.getSeed()).isEqualTo(1);
        assertThat(lido.getCampeonato().getId()).isEqualTo(campeonato.getId());
        assertThat(lido.getOrganizacao().getId()).isEqualTo(org.getId());
        assertThat(repository.findAllByCampeonatoIdAndOrganizacaoId(campeonato.getId(), org.getId())).hasSize(1);
    }

    @Test
    void nomeDuplicadoNoMesmoCampeonatoFalha() {
        Campeonato campeonato = Cenario.pontosCorridos(em, Cenario.organizacao(em, "Org"));
        Cenario.participante(em, campeonato, "Time A");

        assertThatThrownBy(() -> em.persistAndFlush(new Participante(campeonato, "Time A", null)))
                .rootCause().hasMessageContaining("uk_participante_campeonato_nome");
    }

    @Test
    void mesmoNomeEmCampeonatosDiferentesPode() {
        Organizacao org = Cenario.organizacao(em, "Org");
        Campeonato primeiro = Cenario.pontosCorridos(em, org);
        Campeonato segundo = Cenario.mataMata(em, org);
        Cenario.participante(em, primeiro, "Time A");
        Cenario.participante(em, segundo, "Time A");

        em.flush();
        em.clear();

        assertThat(repository.findAllByCampeonatoIdAndOrganizacaoId(primeiro.getId(), org.getId())).hasSize(1);
        assertThat(repository.findAllByCampeonatoIdAndOrganizacaoId(segundo.getId(), org.getId())).hasSize(1);
    }
}
