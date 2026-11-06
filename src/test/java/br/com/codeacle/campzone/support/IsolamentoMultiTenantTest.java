package br.com.codeacle.campzone.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.campeonato.domain.StatusCampeonato;
import br.com.codeacle.campzone.campeonato.repository.CampeonatoRepository;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.participante.domain.Participante;
import br.com.codeacle.campzone.participante.repository.ParticipanteRepository;
import br.com.codeacle.campzone.partida.domain.Confronto;
import br.com.codeacle.campzone.partida.domain.Fase;
import br.com.codeacle.campzone.partida.domain.Partida;
import br.com.codeacle.campzone.partida.domain.Resultado;
import br.com.codeacle.campzone.partida.domain.Rodada;
import br.com.codeacle.campzone.partida.repository.ConfrontoRepository;
import br.com.codeacle.campzone.partida.repository.FaseRepository;
import br.com.codeacle.campzone.partida.repository.PartidaRepository;
import br.com.codeacle.campzone.partida.repository.ResultadoRepository;
import br.com.codeacle.campzone.partida.repository.RodadaRepository;

/** A organização B nunca enxerga, por nenhum método dos repositórios, os dados da organização A. */
@RepositorioTest
class IsolamentoMultiTenantTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private CampeonatoRepository campeonatoRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private FaseRepository faseRepository;
    @Autowired
    private RodadaRepository rodadaRepository;
    @Autowired
    private ConfrontoRepository confrontoRepository;
    @Autowired
    private PartidaRepository partidaRepository;
    @Autowired
    private ResultadoRepository resultadoRepository;

    private Organizacao orgA;
    private Organizacao orgB;
    private Campeonato campeonatoPc;
    private Campeonato campeonatoMm;
    private Participante participante;
    private Fase fasePc;
    private Fase faseMm;
    private Rodada rodada;
    private Confronto confronto;
    private Partida partida;
    private Resultado resultado;

    @BeforeEach
    void criaDadosDaOrganizacaoA() {
        orgA = Cenario.organizacao(em, "Org A");
        orgB = Cenario.organizacao(em, "Org B");

        campeonatoPc = Cenario.pontosCorridos(em, orgA);
        campeonatoMm = Cenario.mataMata(em, orgA);
        participante = Cenario.participante(em, campeonatoPc, "A1");
        Participante adversario = Cenario.participante(em, campeonatoPc, "A2");
        fasePc = em.persist(new Fase(campeonatoPc, "Fase única", 1));
        rodada = em.persist(new Rodada(fasePc, 1));
        partida = em.persist(Partida.daRodada(rodada, participante, adversario));
        resultado = em.persist(new Resultado(partida, 1, 0));
        faseMm = em.persist(new Fase(campeonatoMm, "Final", 1));
        Participante m1 = Cenario.participante(em, campeonatoMm, "M1");
        Participante m2 = Cenario.participante(em, campeonatoMm, "M2");
        confronto = em.persist(Confronto.entre(faseMm, 1, m1, m2));
        em.flush();
        em.clear();
    }

    @Test
    void buscaPorIdComOrganizacaoErradaNaoEncontra() {
        Long b = orgB.getId();

        assertThat(campeonatoRepository.findByIdAndOrganizacaoId(campeonatoPc.getId(), b)).isEmpty();
        assertThat(participanteRepository.findByIdAndOrganizacaoId(participante.getId(), b)).isEmpty();
        assertThat(faseRepository.findByIdAndOrganizacaoId(fasePc.getId(), b)).isEmpty();
        assertThat(rodadaRepository.findByIdAndOrganizacaoId(rodada.getId(), b)).isEmpty();
        assertThat(confrontoRepository.findByIdAndOrganizacaoId(confronto.getId(), b)).isEmpty();
        assertThat(partidaRepository.findByIdAndOrganizacaoId(partida.getId(), b)).isEmpty();
        assertThat(resultadoRepository.findByIdAndOrganizacaoId(resultado.getId(), b)).isEmpty();
        assertThat(resultadoRepository.findByPartidaIdAndOrganizacaoId(partida.getId(), b)).isEmpty();
    }

    @Test
    void buscaPorIdComAOrganizacaoCertaEncontra() {
        Long a = orgA.getId();

        assertThat(campeonatoRepository.findByIdAndOrganizacaoId(campeonatoPc.getId(), a)).isPresent();
        assertThat(participanteRepository.findByIdAndOrganizacaoId(participante.getId(), a)).isPresent();
        assertThat(partidaRepository.findByIdAndOrganizacaoId(partida.getId(), a)).isPresent();
        assertThat(resultadoRepository.findByIdAndOrganizacaoId(resultado.getId(), a)).isPresent();
    }

    @Test
    void listagensDaOrganizacaoBNaoTrazemNadaDeA() {
        Long b = orgB.getId();

        assertThat(campeonatoRepository.findAllByOrganizacaoId(b)).isEmpty();
        assertThat(participanteRepository.findAllByOrganizacaoId(b)).isEmpty();
        assertThat(participanteRepository.findAllByCampeonatoIdAndOrganizacaoId(campeonatoPc.getId(), b)).isEmpty();
        assertThat(faseRepository.findAllByOrganizacaoId(b)).isEmpty();
        assertThat(faseRepository.findAllByCampeonatoIdAndOrganizacaoIdOrderByOrdemAsc(campeonatoPc.getId(), b))
                .isEmpty();
        assertThat(rodadaRepository.findAllByOrganizacaoId(b)).isEmpty();
        assertThat(rodadaRepository.findAllByFaseIdAndOrganizacaoIdOrderByNumeroAsc(fasePc.getId(), b)).isEmpty();
        assertThat(confrontoRepository.findAllByOrganizacaoId(b)).isEmpty();
        assertThat(confrontoRepository.findAllByFaseIdAndOrganizacaoIdOrderByPosicaoAsc(faseMm.getId(), b)).isEmpty();
        assertThat(partidaRepository.findAllByOrganizacaoId(b)).isEmpty();
        assertThat(partidaRepository.findAllByFaseIdAndOrganizacaoId(fasePc.getId(), b)).isEmpty();
        assertThat(resultadoRepository.findAllByOrganizacaoId(b)).isEmpty();
    }

    @Test
    void listagensDaOrganizacaoATrazemSoOsDela() {
        Long a = orgA.getId();

        assertThat(campeonatoRepository.findAllByOrganizacaoId(a)).hasSize(2);
        assertThat(participanteRepository.findAllByOrganizacaoId(a)).hasSize(4);
        assertThat(partidaRepository.findAllByOrganizacaoId(a)).hasSize(1);
        assertThat(resultadoRepository.findAllByOrganizacaoId(a)).hasSize(1);
    }

    @Test
    void contagemDeAtivosEPorOrganizacao() {
        assertThat(campeonatoRepository.countByOrganizacaoIdAndStatusIn(orgB.getId(),
                List.of(StatusCampeonato.RASCUNHO))).isZero();
        assertThat(campeonatoRepository.countByOrganizacaoIdAndStatusIn(orgA.getId(),
                List.of(StatusCampeonato.RASCUNHO))).isEqualTo(2);
    }
}
