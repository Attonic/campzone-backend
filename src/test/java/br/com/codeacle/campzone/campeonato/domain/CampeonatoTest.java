package br.com.codeacle.campzone.campeonato.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.common.domain.InvarianteDominioException;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.plano.domain.Plano;

class CampeonatoTest {

    private final Organizacao org = new Organizacao("Org", new Plano("GRATIS", "Grátis", 1, 8));
    private final ConfigPontosCorridosEmbutida pontos = ConfigPontosCorridosEmbutida.padrao(1);
    private final ConfigMataMataEmbutida mata = new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, null,
            List.of(DesempateMataMata.PENALTIS), Seeding.ALEATORIO, false);

    @Test
    void pontosCorridosNasceComoRascunhoNaoPublicoESoComSuaConfig() {
        Campeonato c = Campeonato.pontosCorridos(org, " Liga ", "Futebol", "liga-1", pontos);

        assertThat(c.getNome()).isEqualTo("Liga");
        assertThat(c.getFormato()).isEqualTo(FormatoCampeonato.PONTOS_CORRIDOS);
        assertThat(c.getStatus()).isEqualTo(StatusCampeonato.RASCUNHO);
        assertThat(c.isPublico()).isFalse();
        assertThat(c.isAtivo()).isTrue();
        assertThat(c.getConfigPontosCorridos()).isSameAs(pontos);
        assertThat(c.getConfigMataMata()).isNull();
        assertThat(c.getSeedSorteio()).isNull();
        assertThat(c.getCriadoEm()).isNotNull();
    }

    @Test
    void mataMataNasceSoComSuaConfig() {
        Campeonato c = Campeonato.mataMata(org, "Copa", "Xadrez", "copa-1", mata);

        assertThat(c.getFormato()).isEqualTo(FormatoCampeonato.MATA_MATA);
        assertThat(c.getConfigMataMata()).isSameAs(mata);
        assertThat(c.getConfigPontosCorridos()).isNull();
    }

    @Test
    void formatoSemAConfigCertaEhRejeitado() {
        assertThatThrownBy(() -> Campeonato.pontosCorridos(org, "Liga", "Futebol", "liga", null))
                .isInstanceOf(InvarianteDominioException.class).hasMessageContaining("PONTOS_CORRIDOS");
        assertThatThrownBy(() -> Campeonato.mataMata(org, "Copa", "Xadrez", "copa", null))
                .isInstanceOf(InvarianteDominioException.class).hasMessageContaining("MATA_MATA");
        assertThatThrownBy(() -> Campeonato.criar(org, "Liga", "Futebol", "liga", FormatoCampeonato.PONTOS_CORRIDOS,
                null, mata)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Campeonato.criar(org, "Copa", "Xadrez", "copa", FormatoCampeonato.MATA_MATA,
                pontos, null)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void formatoComAsDuasConfigsEhRejeitado() {
        assertThatThrownBy(() -> Campeonato.criar(org, "Liga", "Futebol", "liga", FormatoCampeonato.PONTOS_CORRIDOS,
                pontos, mata)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Campeonato.criar(org, "Copa", "Xadrez", "copa", FormatoCampeonato.MATA_MATA,
                pontos, mata)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void camposObrigatorios() {
        assertThatThrownBy(() -> Campeonato.pontosCorridos(null, "Liga", "Futebol", "liga", pontos))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Campeonato.pontosCorridos(org, " ", "Futebol", "liga", pontos))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Campeonato.pontosCorridos(org, "Liga", "", "liga", pontos))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Campeonato.pontosCorridos(org, "Liga", "Futebol", null, pontos))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Campeonato.criar(org, "Liga", "Futebol", "liga", null, pontos, null))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> Campeonato.pontosCorridos(org, "x".repeat(121), "Futebol", "liga", pontos))
                .isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void cicloDeVidaRascunhoEmAndamentoFinalizado() {
        Campeonato c = Campeonato.pontosCorridos(org, "Liga", "Futebol", "liga", pontos);

        assertThatThrownBy(c::finalizar).isInstanceOf(InvarianteDominioException.class);
        c.iniciar();
        assertThat(c.getStatus()).isEqualTo(StatusCampeonato.EM_ANDAMENTO);
        assertThat(c.isAtivo()).isTrue();
        assertThatThrownBy(c::iniciar).isInstanceOf(InvarianteDominioException.class);
        c.finalizar();
        assertThat(c.getStatus()).isEqualTo(StatusCampeonato.FINALIZADO);
        assertThat(c.isAtivo()).isFalse();
    }

    @Test
    void seedDoSorteioSoPodeSerRegistradaUmaVez() {
        Campeonato c = Campeonato.mataMata(org, "Copa", "Xadrez", "copa", mata);

        c.registrarSeedSorteio(7L);

        assertThat(c.getSeedSorteio()).isEqualTo(7L);
        assertThatThrownBy(() -> c.registrarSeedSorteio(8L)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void alteracoesBasicas() {
        Campeonato c = Campeonato.mataMata(org, "Copa", "Xadrez", "copa", mata);

        c.renomear("Copa 2");
        c.alterarModalidade("Esports");
        c.definirPublico(true);

        assertThat(c.getNome()).isEqualTo("Copa 2");
        assertThat(c.getModalidade()).isEqualTo("Esports");
        assertThat(c.isPublico()).isTrue();
        assertThatThrownBy(() -> c.renomear(" ")).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> c.alterarModalidade(null)).isInstanceOf(InvarianteDominioException.class);
    }
}
