package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.GOLS_FORA;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PRORROGACAO;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.SORTEIO;
import static br.com.codeacle.campzone.motor.matamata.TipoConfronto.IDA_E_VOLTA;
import static br.com.codeacle.campzone.motor.matamata.TipoConfronto.JOGO_UNICO;
import static br.com.codeacle.campzone.motor.matamata.TipoConfronto.SERIE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;

class ConfigMataMataTest {

    private static ConfigMataMata config(TipoConfronto tipo, Integer melhorDe, DesempateMataMata... desempates) {
        return new ConfigMataMata(tipo, melhorDe, List.of(desempates), Seeding.ALEATORIO, false);
    }

    // ---------------------------------------------------------------- válidas

    @Test
    void aceitaOsTresTiposDeConfronto() {
        assertThatCode(() -> config(JOGO_UNICO, null, PENALTIS)).doesNotThrowAnyException();
        assertThatCode(() -> config(IDA_E_VOLTA, null, GOLS_FORA, PENALTIS)).doesNotThrowAnyException();
        assertThatCode(() -> config(SERIE, 3, PENALTIS)).doesNotThrowAnyException();
    }

    @Test
    void guardaTodosOsCampos() {
        ConfigMataMata config = new ConfigMataMata(SERIE, 5, List.of(PRORROGACAO, SORTEIO), Seeding.MANUAL, true);

        assertThat(config.tipoConfronto()).isEqualTo(SERIE);
        assertThat(config.melhorDe()).isEqualTo(5);
        assertThat(config.desempates()).containsExactly(PRORROGACAO, SORTEIO);
        assertThat(config.seeding()).isEqualTo(Seeding.MANUAL);
        assertThat(config.disputaTerceiroLugar()).isTrue();
    }

    @Test
    void aceitaQualquerCriterioTerminalComoUnicoCriterio() {
        assertThatCode(() -> config(JOGO_UNICO, null, PENALTIS)).doesNotThrowAnyException();
        assertThatCode(() -> config(JOGO_UNICO, null, SORTEIO)).doesNotThrowAnyException();
    }

    @Test
    void aceitaGolsForaNaIdaEVoltaEmQualquerPosicaoAntesDoSorteio() {
        assertThatCode(() -> config(IDA_E_VOLTA, null, GOLS_FORA, PRORROGACAO, PENALTIS)).doesNotThrowAnyException();
        assertThatCode(() -> config(IDA_E_VOLTA, null, PRORROGACAO, GOLS_FORA, SORTEIO)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 7, 9, 101 })
    void aceitaSerieImparDeTresOuMais(int melhorDe) {
        assertThatCode(() -> config(SERIE, melhorDe, SORTEIO)).doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------- campos obrigatórios

    @Test
    void recusaTipoDeConfrontoOuSeedingNulos() {
        assertThatThrownBy(() -> new ConfigMataMata(null, null, List.of(PENALTIS), Seeding.ALEATORIO, false))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("tipo de confronto");
        assertThatThrownBy(() -> new ConfigMataMata(JOGO_UNICO, null, List.of(PENALTIS), null, false))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("seeding");
    }

    // ---------------------------------------------------------------- melhorDe

    @Test
    void recusaMelhorDeForaDaSerie() {
        assertThatThrownBy(() -> config(JOGO_UNICO, 3, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("melhorDe")
                .hasMessageContaining("JOGO_UNICO");
        assertThatThrownBy(() -> config(IDA_E_VOLTA, 3, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("melhorDe");
    }

    @Test
    void recusaSerieSemMelhorDe() {
        assertThatThrownBy(() -> config(SERIE, null, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("melhorDe");
    }

    @ParameterizedTest
    @ValueSource(ints = { -3, 0, 1, 2, 4, 6, 100 })
    void recusaSerieComMelhorDeParOuMenorQueTres(int melhorDe) {
        assertThatThrownBy(() -> config(SERIE, melhorDe, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("ímpar")
                .hasMessageContaining(String.valueOf(melhorDe));
    }

    // ---------------------------------------------------------------- desempates

    @Test
    void recusaDesempatesNulosVaziosOuComItemNulo() {
        assertThatThrownBy(() -> new ConfigMataMata(JOGO_UNICO, null, null, Seeding.ALEATORIO, false))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("critério");
        assertThatThrownBy(() -> new ConfigMataMata(JOGO_UNICO, null, List.of(), Seeding.ALEATORIO, false))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("critério");
        assertThatThrownBy(() -> new ConfigMataMata(JOGO_UNICO, null, Arrays.asList(PENALTIS, null),
                Seeding.ALEATORIO, false)).isInstanceOf(ConfigInvalidaException.class);
    }

    @Test
    void recusaDesempatesRepetidos() {
        assertThatThrownBy(() -> config(JOGO_UNICO, null, PRORROGACAO, PRORROGACAO, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("repetir");
    }

    @Test
    void recusaGolsForaForaDaIdaEVolta() {
        assertThatThrownBy(() -> config(JOGO_UNICO, null, GOLS_FORA, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("GOLS_FORA");
        assertThatThrownBy(() -> config(SERIE, 3, GOLS_FORA, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("GOLS_FORA");
    }

    @Test
    void recusaSorteioForaDaUltimaPosicao() {
        assertThatThrownBy(() -> config(JOGO_UNICO, null, SORTEIO, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("SORTEIO");
        assertThatThrownBy(() -> config(JOGO_UNICO, null, PRORROGACAO, SORTEIO, PENALTIS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("SORTEIO");
    }

    @Test
    void recusaListaSemCriterioTerminal() {
        assertThatThrownBy(() -> config(JOGO_UNICO, null, PRORROGACAO))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("PENALTIS ou SORTEIO");
        assertThatThrownBy(() -> config(IDA_E_VOLTA, null, GOLS_FORA))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("PENALTIS ou SORTEIO");
        assertThatThrownBy(() -> config(IDA_E_VOLTA, null, GOLS_FORA, PRORROGACAO))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("PENALTIS ou SORTEIO");
    }

    // ---------------------------------------------------------------- imutabilidade

    @Test
    void copiaOsDesempatesEHaImutabilidade() {
        List<DesempateMataMata> original = new ArrayList<>(List.of(PRORROGACAO, PENALTIS));
        ConfigMataMata config = new ConfigMataMata(JOGO_UNICO, null, original, Seeding.ALEATORIO, false);

        original.add(SORTEIO);

        assertThat(config.desempates()).containsExactly(PRORROGACAO, PENALTIS);
        assertThatThrownBy(() -> config.desempates().add(SORTEIO)).isInstanceOf(UnsupportedOperationException.class);
    }
}
