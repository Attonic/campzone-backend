package br.com.codeacle.campzone.campeonato.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.common.domain.InvarianteDominioException;

class ConfigsEmbutidasTest {

    private static final List<CriterioDesempate> CRITERIOS = List.of(CriterioDesempate.PONTOS, CriterioDesempate.SALDO);

    @Test
    void pontosCorridosPadraoUsa310() {
        ConfigPontosCorridosEmbutida config = ConfigPontosCorridosEmbutida.padrao(2);

        assertThat(config.getTurnos()).isEqualTo(2);
        assertThat(config.getPontosVitoria()).isEqualTo(3);
        assertThat(config.getPontosEmpate()).isEqualTo(1);
        assertThat(config.getPontosDerrota()).isZero();
        assertThat(config.isPermiteEmpate()).isTrue();
        assertThat(config.getCriteriosDesempate()).containsExactly(CriterioDesempate.PONTOS,
                CriterioDesempate.VITORIAS, CriterioDesempate.SALDO, CriterioDesempate.PRO,
                CriterioDesempate.CONFRONTO_DIRETO, CriterioDesempate.SORTEIO);
    }

    @Test
    void turnosMenoresQueUmSaoRejeitados() {
        assertThatThrownBy(() -> new ConfigPontosCorridosEmbutida(0, 3, 1, 0, true, CRITERIOS))
                .isInstanceOf(InvarianteDominioException.class).hasMessageContaining("Turnos");
        assertThatThrownBy(() -> ConfigPontosCorridosEmbutida.padrao(-1))
                .isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void criteriosInvalidosSaoRejeitados() {
        assertThatThrownBy(() -> new ConfigPontosCorridosEmbutida(1, 3, 1, 0, true, null))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new ConfigPontosCorridosEmbutida(1, 3, 1, 0, true,
                List.of(CriterioDesempate.SALDO, CriterioDesempate.SALDO)))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new ConfigPontosCorridosEmbutida(1, 3, 1, 0, true,
                Arrays.asList(CriterioDesempate.SALDO, null)))
                .isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void criteriosSaoCopiadosEExpostosSomenteParaLeitura() {
        List<CriterioDesempate> origem = new ArrayList<>(CRITERIOS);
        ConfigPontosCorridosEmbutida config = new ConfigPontosCorridosEmbutida(1, 3, 1, 0, true, origem);
        origem.add(CriterioDesempate.SORTEIO);

        assertThat(config.getCriteriosDesempate()).containsExactlyElementsOf(CRITERIOS);
        assertThatThrownBy(() -> config.getCriteriosDesempate().add(CriterioDesempate.PRO))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mataMataJogoUnicoNaoTemMelhorDe() {
        ConfigMataMataEmbutida config = new ConfigMataMataEmbutida(TipoConfronto.IDA_E_VOLTA, null,
                List.of(DesempateMataMata.GOLS_FORA, DesempateMataMata.PENALTIS), Seeding.MANUAL, true);

        assertThat(config.getMelhorDe()).isNull();
        assertThat(config.getDesempates()).containsExactly(DesempateMataMata.GOLS_FORA, DesempateMataMata.PENALTIS);
        assertThat(config.getSeeding()).isEqualTo(Seeding.MANUAL);
        assertThat(config.isDisputaTerceiroLugar()).isTrue();
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, 3,
                List.of(DesempateMataMata.SORTEIO), Seeding.MANUAL, false)).isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void serieExigeMelhorDeImparMaiorOuIgualATres() {
        assertThat(new ConfigMataMataEmbutida(TipoConfronto.SERIE, 3, List.of(DesempateMataMata.SORTEIO), Seeding.ALEATORIO, false)
                .getMelhorDe()).isEqualTo(3);
        assertThat(new ConfigMataMataEmbutida(TipoConfronto.SERIE, 7, List.of(DesempateMataMata.SORTEIO), Seeding.ALEATORIO, false)
                .getMelhorDe()).isEqualTo(7);
        for (Integer invalido : new Integer[] { null, 1, 2, 4, 6, 0, -3 }) {
            assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.SERIE, invalido, List.of(DesempateMataMata.SORTEIO),
                    Seeding.ALEATORIO, false)).as("melhorDe=%s", invalido)
                    .isInstanceOf(InvarianteDominioException.class);
        }
    }

    @Test
    void mataMataExigeTipoSeedingEDesempatesValidos() {
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(null, null, List.of(DesempateMataMata.SORTEIO), Seeding.ALEATORIO, false))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, null, List.of(DesempateMataMata.SORTEIO), null, false))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, null, null,
                Seeding.ALEATORIO, false)).isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, null,
                List.of(DesempateMataMata.SORTEIO, DesempateMataMata.SORTEIO), Seeding.ALEATORIO, false))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, null,
                Arrays.asList(DesempateMataMata.SORTEIO, null), Seeding.ALEATORIO, false))
                .isInstanceOf(InvarianteDominioException.class);
    }

    @Test
    void pontuacaoInvalidaEhRejeitada() {
        assertThatThrownBy(() -> new ConfigPontosCorridosEmbutida(1, 3, 1, -1, true, CRITERIOS))
                .isInstanceOf(InvarianteDominioException.class);
        assertThatThrownBy(() -> new ConfigPontosCorridosEmbutida(1, 1, 3, 0, true, CRITERIOS))
                .isInstanceOf(InvarianteDominioException.class).hasMessageContaining("vitória >= empate >= derrota");
        assertThatThrownBy(() -> new ConfigPontosCorridosEmbutida(1, 3, 0, 1, true, CRITERIOS))
                .isInstanceOf(InvarianteDominioException.class);
        // pontuação de xadrez (1 / 0,5 aproximado em inteiros) e zeros são válidas
        assertThat(new ConfigPontosCorridosEmbutida(1, 2, 1, 0, true, CRITERIOS).getPontosVitoria()).isEqualTo(2);
        assertThat(new ConfigPontosCorridosEmbutida(1, 0, 0, 0, true, CRITERIOS).getPontosVitoria()).isZero();
    }

    @Test
    void listaDeCriteriosVaziaEhRejeitada() {
        assertThatThrownBy(() -> new ConfigPontosCorridosEmbutida(1, 3, 1, 0, true, List.of()))
                .isInstanceOf(InvarianteDominioException.class).hasMessageContaining("critério");
    }

    @Test
    void listaDeDesempatesDoMataMataVaziaEhRejeitada() {
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, null, List.of(),
                Seeding.ALEATORIO, false)).isInstanceOf(InvarianteDominioException.class)
                .hasMessageContaining("desempate");
    }

    @Test
    void golsForaSoVaiComIdaEVolta() {
        List<DesempateMataMata> desempates = List.of(DesempateMataMata.GOLS_FORA, DesempateMataMata.PENALTIS);

        assertThat(new ConfigMataMataEmbutida(TipoConfronto.IDA_E_VOLTA, null, desempates, Seeding.ALEATORIO, false)
                .getDesempates()).containsExactlyElementsOf(desempates);
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, null, desempates,
                Seeding.ALEATORIO, false)).isInstanceOf(InvarianteDominioException.class)
                .hasMessageContaining("GOLS_FORA");
        assertThatThrownBy(() -> new ConfigMataMataEmbutida(TipoConfronto.SERIE, 3, desempates,
                Seeding.ALEATORIO, false)).isInstanceOf(InvarianteDominioException.class);
    }
}
