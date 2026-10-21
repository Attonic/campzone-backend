package br.com.codeacle.campzone.partida.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.campeonato.domain.ConfigMataMataEmbutida;
import br.com.codeacle.campzone.campeonato.domain.ConfigPontosCorridosEmbutida;
import br.com.codeacle.campzone.campeonato.domain.Seeding;
import br.com.codeacle.campzone.campeonato.domain.TipoConfronto;
import br.com.codeacle.campzone.common.domain.InvarianteDominioException;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.participante.domain.Participante;
import br.com.codeacle.campzone.plano.domain.Plano;

class PartidaDominioTest {

    private final Organizacao org = new Organizacao("Org", new Plano("GRATIS", "Grátis", 1, 8));
    private final Campeonato liga = Campeonato.pontosCorridos(org, "Liga", "Futebol", "liga",
            ConfigPontosCorridosEmbutida.padrao(1));
    private final Campeonato copa = Campeonato.mataMata(org, "Copa", "Xadrez", "copa",
            new ConfigMataMataEmbutida(TipoConfronto.IDA_E_VOLTA, null, List.of(), Seeding.ALEATORIO, false));
    private final Participante a = new Participante(liga, "A", null);
    private final Participante b = new Participante(liga, "B", null);
    private final Fase fase = new Fase(liga, "Fase única", 1);
    private final Fase faseCopa = new Fase(copa, "Final", 1);

    @Test
    void entidadesFilhasHerdamAOrganizacao() {
        Rodada rodada = new Rodada(fase, 1);
        Partida partida = Partida.daRodada(rodada, a, b);

        assertThat(a.getOrganizacao()).isSameAs(org);
        assertThat(fase.getOrganizacao()).isSameAs(org);
        assertThat(rodada.getOrganizacao()).isSameAs(org);
        assertThat(partida.getOrganizacao()).isSameAs(org);
        assertThat(partida.getFase()).isSameAs(fase);
        assertThat(new Resultado(partida, 1, 0).getOrganizacao()).isSameAs(org);
    }

    @Test
    void participanteExigeNomeESeedValida() {
        assertThatThrownBy(() -> new Participante(liga, " ", null)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Participante(null, "X", null)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Participante(liga, "X", 0)).isInstanceOf(InvarianteDominioException.class);
        a.definirSeed(2);
        assertThat(a.getSeed()).isEqualTo(2);
        assertThatThrownBy(() -> a.definirSeed(-1)).isInstanceOf(InvarianteDominioException.class);
        a.renomear(" Novo ");
        assertThat(a.getNome()).isEqualTo("Novo");
    }

    @Test
    void faseExigeNomeEOrdemEControlaOStatus() {
        assertThatThrownBy(() -> new Fase(liga, "", 1)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Fase(liga, "F", 0)).isInstanceOf(InvarianteDominioException.class);
        assertThat(fase.getStatus()).isEqualTo(StatusFase.PENDENTE);
        assertThatThrownBy(fase::encerrar).isInstanceOf(InvarianteDominioException.class);
        fase.iniciar();
        assertThatThrownBy(fase::iniciar).isInstanceOf(InvarianteDominioException.class);
        fase.encerrar();
        assertThat(fase.getStatus()).isEqualTo(StatusFase.ENCERRADA);
    }

    @Test
    void rodadaExigeNumeroPositivo() {
        assertThatThrownBy(() -> new Rodada(fase, 0)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Rodada(null, 1)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void partidaComMandanteIgualAoVisitanteEhRejeitada() {
        Rodada rodada = new Rodada(fase, 1);

        assertThatThrownBy(() -> Partida.daRodada(rodada, a, a))
                .isInstanceOf(InvarianteDominioException.class).hasMessageContaining("diferentes");
    }

    @Test
    void partidaExigeParticipantesERodadaOuConfronto() {
        Rodada rodada = new Rodada(fase, 1);

        assertThatThrownBy(() -> Partida.daRodada(rodada, null, b)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Partida.daRodada(rodada, a, null)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Partida.daRodada(null, a, b)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Partida.doConfronto(null, 1, a, b)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void partidaDoConfrontoExigeNumeroDoJogoPositivo() {
        Confronto confronto = Confronto.entre(faseCopa, 1, a, b);

        Partida ida = Partida.doConfronto(confronto, 1, a, b);

        assertThat(ida.getConfronto()).isSameAs(confronto);
        assertThat(ida.getRodada()).isNull();
        assertThat(ida.getFase()).isSameAs(faseCopa);
        assertThat(ida.getStatus()).isEqualTo(StatusPartida.AGENDADA);
        assertThatThrownBy(() -> Partida.doConfronto(confronto, 0, a, b))
                .isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void partidaAgendaEEncerra() {
        Partida partida = Partida.daRodada(new Rodada(fase, 1), a, b);
        Instant quando = Instant.parse("2026-11-01T20:00:00Z");

        partida.agendar(quando);
        assertThat(partida.getDataHora()).isEqualTo(quando);
        partida.encerrar();
        assertThat(partida.getStatus()).isEqualTo(StatusPartida.ENCERRADA);
        assertThatThrownBy(partida::encerrar).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> partida.agendar(quando)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void confrontoEntreDoisDistintosEComBye() {
        Confronto confronto = Confronto.entre(faseCopa, 1, a, b);
        assertThat(confronto.isBye()).isFalse();
        assertThat(confronto.getVencedor()).isNull();
        assertThat(confronto.getOrganizacao()).isSameAs(org);

        confronto.definirVencedor(b);
        assertThat(confronto.getVencedor()).isSameAs(b);

        Confronto bye = Confronto.comBye(faseCopa, 2, a);
        assertThat(bye.isBye()).isTrue();
        assertThat(bye.getParticipanteB()).isNull();
        assertThat(bye.getVencedor()).isSameAs(a);
    }

    @Test
    void confrontoInvalidoEhRejeitado() {
        Participante estranho = new Participante(copa, "Z", null);

        assertThatThrownBy(() -> Confronto.entre(faseCopa, 1, a, a)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Confronto.entre(faseCopa, 1, a, null)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Confronto.entre(faseCopa, 1, null, b)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Confronto.entre(faseCopa, 0, a, b)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Confronto.comBye(faseCopa, 1, null)).isInstanceOf(InvarianteDominioException.class);
        Confronto confronto = Confronto.entre(faseCopa, 1, a, b);
        assertThatThrownBy(() -> confronto.definirVencedor(estranho)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> confronto.definirVencedor(null)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Confronto.comBye(faseCopa, 1, a).definirVencedor(a))
                .isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void resultadoComPlacarNegativoEhRejeitado() {
        Partida partida = Partida.daRodada(new Rodada(fase, 1), a, b);

        assertThatThrownBy(() -> new Resultado(partida, -1, 0)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Resultado(partida, 0, -1)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Resultado(partida, 1, 1, -1, 0)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Resultado(partida, 1, 1, 0, -1)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Resultado(null, 1, 1)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void resultadoExigeDesempateNosDoisLadosOuEmNenhum() {
        Partida partida = Partida.daRodada(new Rodada(fase, 1), a, b);

        assertThatThrownBy(() -> new Resultado(partida, 1, 1, 3, null))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new Resultado(partida, 1, 1, null, 3))
                .isInstanceOf(InvarianteDominioException.class);
        Resultado resultado = new Resultado(partida, 1, 1, 5, 4);
        assertThat(resultado.getPlacarDesempateMandante()).isEqualTo(5);
        assertThat(resultado.getPlacarDesempateVisitante()).isEqualTo(4);
    }

    @Test
    void resultadoPodeSerCorrigidoSemPerderORegistro() {
        Resultado resultado = new Resultado(Partida.daRodada(new Rodada(fase, 1), a, b), 1, 0);
        Instant registrado = resultado.getRegistradoEm();

        resultado.corrigir(2, 2, 3, 1);

        assertThat(resultado.getPlacarMandante()).isEqualTo(2);
        assertThat(resultado.getPlacarDesempateMandante()).isEqualTo(3);
        assertThat(resultado.getRegistradoEm()).isEqualTo(registrado);
        assertThatThrownBy(() -> resultado.corrigir(-1, 0, null, null))
                .isInstanceOf(InvarianteDominioException.class);
    }
}
