package br.com.codeacle.campzone.partida.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.participante.domain.Participante;
import br.com.codeacle.campzone.partida.domain.Confronto;
import br.com.codeacle.campzone.partida.domain.Fase;
import br.com.codeacle.campzone.partida.domain.Partida;
import br.com.codeacle.campzone.partida.domain.Resultado;
import br.com.codeacle.campzone.partida.domain.Rodada;
import br.com.codeacle.campzone.partida.domain.StatusFase;
import br.com.codeacle.campzone.partida.domain.StatusPartida;
import br.com.codeacle.campzone.support.Cenario;
import br.com.codeacle.campzone.support.RepositorioTest;

@RepositorioTest
class PartidaRepositoryTest {

    @Autowired
    private TestEntityManager em;

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

    @Test
    void persisteELeAEstruturaDePontosCorridosComResultado() {
        Organizacao org = Cenario.organizacao(em, "Org");
        Campeonato campeonato = Cenario.pontosCorridos(em, org);
        Participante a = Cenario.participante(em, campeonato, "A");
        Participante b = Cenario.participante(em, campeonato, "B");

        Fase fase = faseRepository.save(new Fase(campeonato, "Fase única", 1));
        Rodada rodada = rodadaRepository.save(new Rodada(fase, 1));
        Instant quando = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Partida partida = Partida.daRodada(rodada, a, b);
        partida.agendar(quando);
        partidaRepository.save(partida);
        Resultado resultado = resultadoRepository.save(new Resultado(partida, 2, 1));
        partida.encerrar();
        em.flush();
        em.clear();

        Fase faseLida = faseRepository.findByIdAndOrganizacaoId(fase.getId(), org.getId()).orElseThrow();
        assertThat(faseLida.getNome()).isEqualTo("Fase única");
        assertThat(faseLida.getStatus()).isEqualTo(StatusFase.PENDENTE);
        assertThat(faseLida.getCampeonato().getId()).isEqualTo(campeonato.getId());

        assertThat(rodadaRepository.findAllByFaseIdAndOrganizacaoIdOrderByNumeroAsc(fase.getId(), org.getId()))
                .extracting(Rodada::getNumero).containsExactly(1);

        Partida lida = partidaRepository.findByIdAndOrganizacaoId(partida.getId(), org.getId()).orElseThrow();
        assertThat(lida.getRodada().getId()).isEqualTo(rodada.getId());
        assertThat(lida.getConfronto()).isNull();
        assertThat(lida.getNumeroJogo()).isEqualTo(1);
        assertThat(lida.getMandante().getId()).isEqualTo(a.getId());
        assertThat(lida.getVisitante().getId()).isEqualTo(b.getId());
        assertThat(lida.getStatus()).isEqualTo(StatusPartida.ENCERRADA);
        assertThat(lida.getDataHora()).isEqualTo(quando);
        assertThat(lida.getVersion()).isPositive();

        Resultado resultadoLido = resultadoRepository
                .findByPartidaIdAndOrganizacaoId(partida.getId(), org.getId()).orElseThrow();
        assertThat(resultadoLido.getId()).isEqualTo(resultado.getId());
        assertThat(resultadoLido.getPlacarMandante()).isEqualTo(2);
        assertThat(resultadoLido.getPlacarVisitante()).isEqualTo(1);
        assertThat(resultadoLido.getPlacarDesempateMandante()).isNull();
        assertThat(resultadoLido.getRegistradoEm()).isNotNull();
    }

    @Test
    void persisteELeAEstruturaDeMataMataComIdaEVoltaDesempateEBye() {
        Organizacao org = Cenario.organizacao(em, "Org");
        Campeonato campeonato = Cenario.mataMata(em, org);
        Participante a = Cenario.participante(em, campeonato, "A");
        Participante b = Cenario.participante(em, campeonato, "B");
        Participante c = Cenario.participante(em, campeonato, "C");

        Fase fase = faseRepository.save(new Fase(campeonato, "Semifinal", 1));
        Confronto confronto = confrontoRepository.save(Confronto.entre(fase, 1, a, b));
        Confronto bye = confrontoRepository.save(Confronto.comBye(fase, 2, c));
        Partida ida = partidaRepository.save(Partida.doConfronto(confronto, 1, a, b));
        Partida volta = partidaRepository.save(Partida.doConfronto(confronto, 2, b, a));
        resultadoRepository.save(new Resultado(ida, 1, 1, 4, 3));
        confronto.definirVencedor(a);
        em.flush();
        em.clear();

        var confrontos = confrontoRepository.findAllByFaseIdAndOrganizacaoIdOrderByPosicaoAsc(fase.getId(),
                org.getId());
        assertThat(confrontos).extracting(Confronto::getPosicao).containsExactly(1, 2);
        assertThat(confrontos.get(0).getVencedor().getId()).isEqualTo(a.getId());
        assertThat(confrontos.get(0).getParticipanteB().getId()).isEqualTo(b.getId());
        assertThat(confrontos.get(0).isBye()).isFalse();
        assertThat(confrontos.get(1).isBye()).isTrue();
        assertThat(confrontos.get(1).getParticipanteB()).isNull();
        assertThat(confrontos.get(1).getVencedor().getId()).isEqualTo(c.getId());
        assertThat(bye.getId()).isEqualTo(confrontos.get(1).getId());

        var jogos = partidaRepository.findAllByConfrontoIdAndOrganizacaoIdOrderByNumeroJogoAsc(confronto.getId(),
                org.getId());
        assertThat(jogos).extracting(Partida::getNumeroJogo).containsExactly(1, 2);
        assertThat(jogos.get(1).getMandante().getId()).isEqualTo(b.getId());
        assertThat(jogos.get(0).getRodada()).isNull();
        assertThat(volta.getId()).isEqualTo(jogos.get(1).getId());

        Resultado resultado = resultadoRepository.findByPartidaIdAndOrganizacaoId(ida.getId(), org.getId())
                .orElseThrow();
        assertThat(resultado.getPlacarDesempateMandante()).isEqualTo(4);
        assertThat(resultado.getPlacarDesempateVisitante()).isEqualTo(3);
    }

    @Test
    void faseComMesmaOrdemNoCampeonatoFalha() {
        Campeonato campeonato = Cenario.pontosCorridos(em, Cenario.organizacao(em, "Org"));
        em.persist(new Fase(campeonato, "Fase 1", 1));

        assertThatThrownBy(() -> em.persistAndFlush(new Fase(campeonato, "Outra", 1)))
                .rootCause().hasMessageContaining("uk_fase_campeonato_ordem");
    }

    @Test
    void rodadaComMesmoNumeroNaFaseFalha() {
        Campeonato campeonato = Cenario.pontosCorridos(em, Cenario.organizacao(em, "Org"));
        Fase fase = em.persist(new Fase(campeonato, "Fase 1", 1));
        em.persist(new Rodada(fase, 1));

        assertThatThrownBy(() -> em.persistAndFlush(new Rodada(fase, 1)))
                .rootCause().hasMessageContaining("uk_rodada_fase_numero");
    }

    @Test
    void confrontoNaMesmaPosicaoDaFaseFalha() {
        Campeonato campeonato = Cenario.mataMata(em, Cenario.organizacao(em, "Org"));
        Fase fase = em.persist(new Fase(campeonato, "Final", 1));
        Participante a = Cenario.participante(em, campeonato, "A");
        Participante b = Cenario.participante(em, campeonato, "B");
        em.persist(Confronto.entre(fase, 1, a, b));

        assertThatThrownBy(() -> em.persistAndFlush(Confronto.comBye(fase, 1, a)))
                .rootCause().hasMessageContaining("uk_confronto_fase_posicao");
    }

    @Test
    void numeroDoJogoRepetidoNoConfrontoFalha() {
        Campeonato campeonato = Cenario.mataMata(em, Cenario.organizacao(em, "Org"));
        Fase fase = em.persist(new Fase(campeonato, "Final", 1));
        Participante a = Cenario.participante(em, campeonato, "A");
        Participante b = Cenario.participante(em, campeonato, "B");
        Confronto confronto = em.persist(Confronto.entre(fase, 1, a, b));
        em.persist(Partida.doConfronto(confronto, 1, a, b));

        assertThatThrownBy(() -> em.persistAndFlush(Partida.doConfronto(confronto, 1, b, a)))
                .rootCause().hasMessageContaining("uk_partida_confronto_numero_jogo");
    }

    @Test
    void partidaTemNoMaximoUmResultado() {
        Campeonato campeonato = Cenario.pontosCorridos(em, Cenario.organizacao(em, "Org"));
        Fase fase = em.persist(new Fase(campeonato, "Fase 1", 1));
        Rodada rodada = em.persist(new Rodada(fase, 1));
        Partida partida = em.persist(Partida.daRodada(rodada, Cenario.participante(em, campeonato, "A"),
                Cenario.participante(em, campeonato, "B")));
        em.persist(new Resultado(partida, 0, 0));

        assertThatThrownBy(() -> em.persistAndFlush(new Resultado(partida, 1, 0)))
                .rootCause().hasMessageContaining("uk_resultado_partida");
    }

    @Test
    void bancoRejeitaPartidaSemRodadaENemConfronto() {
        Campeonato campeonato = Cenario.pontosCorridos(em, Cenario.organizacao(em, "Org"));
        Fase fase = em.persist(new Fase(campeonato, "Fase 1", 1));
        Rodada rodada = em.persist(new Rodada(fase, 1));
        Partida partida = em.persist(Partida.daRodada(rodada, Cenario.participante(em, campeonato, "A"),
                Cenario.participante(em, campeonato, "B")));
        em.flush();

        assertThatThrownBy(() -> {
            em.getEntityManager().createNativeQuery("update partida set rodada_id = null where id = :id")
                    .setParameter("id", partida.getId()).executeUpdate();
            em.flush();
        }).rootCause().hasMessageContaining("ck_partida_rodada_xor_confronto");
    }

    @Test
    void bancoRejeitaPartidaComRodadaEConfrontoAoMesmoTempo() {
        Organizacao org = Cenario.organizacao(em, "Org");
        Campeonato liga = Cenario.pontosCorridos(em, org);
        Campeonato copa = Cenario.mataMata(em, org);
        Fase faseLiga = em.persist(new Fase(liga, "Fase 1", 1));
        Rodada rodada = em.persist(new Rodada(faseLiga, 1));
        Partida partida = em.persist(Partida.daRodada(rodada, Cenario.participante(em, liga, "A"),
                Cenario.participante(em, liga, "B")));
        Fase faseCopa = em.persist(new Fase(copa, "Final", 1));
        Confronto confronto = em.persist(Confronto.entre(faseCopa, 1, Cenario.participante(em, copa, "C"),
                Cenario.participante(em, copa, "D")));
        em.flush();

        assertThatThrownBy(() -> em.getEntityManager()
                .createNativeQuery("update partida set confronto_id = :c where id = :id")
                .setParameter("c", confronto.getId()).setParameter("id", partida.getId()).executeUpdate())
                .rootCause().hasMessageContaining("ck_partida_rodada_xor_confronto");
    }

    private Resultado resultadoPersistido() {
        Campeonato campeonato = Cenario.pontosCorridos(em, Cenario.organizacao(em, "Org"));
        Fase fase = em.persist(new Fase(campeonato, "Fase 1", 1));
        Rodada rodada = em.persist(new Rodada(fase, 1));
        Partida partida = em.persist(Partida.daRodada(rodada, Cenario.participante(em, campeonato, "A"),
                Cenario.participante(em, campeonato, "B")));
        Resultado resultado = em.persist(new Resultado(partida, 1, 0));
        em.flush();
        return resultado;
    }

    @Test
    void bancoRejeitaPlacarNegativo() {
        Resultado resultado = resultadoPersistido();

        assertThatThrownBy(() -> em.getEntityManager()
                .createNativeQuery("update resultado set placar_visitante = -1 where id = :id")
                .setParameter("id", resultado.getId()).executeUpdate())
                .rootCause().hasMessageContaining("ck_resultado_placares");
    }

    /** Teste separado: o erro do anterior aborta a transação, então cada violação precisa do seu rollback. */
    @Test
    void bancoRejeitaPlacarDeDesempateNegativo() {
        Resultado resultado = resultadoPersistido();

        assertThatThrownBy(() -> em.getEntityManager()
                .createNativeQuery("update resultado set placar_desempate_mandante = -2 where id = :id")
                .setParameter("id", resultado.getId()).executeUpdate())
                .rootCause().hasMessageContaining("ck_resultado_placares");
    }

    @Test
    void faseEConfrontoTemVersaoParaLockOtimista() {
        Campeonato campeonato = Cenario.mataMata(em, Cenario.organizacao(em, "Org"));
        Fase fase = em.persist(new Fase(campeonato, "Final", 1));
        Confronto confronto = em.persist(Confronto.entre(fase, 1, Cenario.participante(em, campeonato, "A"),
                Cenario.participante(em, campeonato, "B")));
        em.flush();
        assertThat(fase.getVersion()).isZero();
        assertThat(confronto.getVersion()).isZero();

        fase.iniciar();
        confronto.definirVencedor(confronto.getParticipanteA());
        em.flush();

        assertThat(fase.getVersion()).isEqualTo(1L);
        assertThat(confronto.getVersion()).isEqualTo(1L);
    }
}
