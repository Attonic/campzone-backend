package br.com.codeacle.campzone.motor.pontoscorridos;

import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.CONFRONTO_DIRETO;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.PONTOS;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.PRO;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.SALDO;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.SORTEIO;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.VITORIAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;

class ConfigPontosCorridosTest {

    private static final List<CriterioDesempate> CRITERIOS = List.of(PONTOS, SALDO);

    @Test
    void padraoTem310EmpatePermitidoECriteriosCompletos() {
        ConfigPontosCorridos config = ConfigPontosCorridos.padrao(2);

        assertThat(config.turnos()).isEqualTo(2);
        assertThat(config.pontosVitoria()).isEqualTo(3);
        assertThat(config.pontosEmpate()).isEqualTo(1);
        assertThat(config.pontosDerrota()).isZero();
        assertThat(config.permiteEmpate()).isTrue();
        assertThat(config.criterios()).containsExactly(PONTOS, VITORIAS, SALDO, PRO, CONFRONTO_DIRETO, SORTEIO);
    }

    @Test
    void padraoRecusaTurnosInvalidos() {
        assertThatThrownBy(() -> ConfigPontosCorridos.padrao(0)).isInstanceOf(ConfigInvalidaException.class);
    }

    @Test
    void recusaMenosDeUmTurno() {
        assertThatThrownBy(() -> new ConfigPontosCorridos(0, 3, 1, 0, true, CRITERIOS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("turnos");
        assertThatThrownBy(() -> new ConfigPontosCorridos(-1, 3, 1, 0, true, CRITERIOS))
                .isInstanceOf(ConfigInvalidaException.class);
    }

    @Test
    void recusaVitoriaQueNaoSuperaADerrota() {
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 1, 1, 1, true, CRITERIOS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("Pontos");
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 0, 0, 1, true, CRITERIOS))
                .isInstanceOf(ConfigInvalidaException.class);
    }

    @Test
    void recusaEmpateAcimaDaVitoria() {
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 1, 2, 0, true, CRITERIOS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("Pontos");
    }

    @Test
    void recusaEmpateAbaixoDaDerrota() {
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 3, -1, 0, true, CRITERIOS))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("Pontos");
    }

    @Test
    void aceitaPontuacoesNoLimiteDasRegras() {
        assertThatCode(() -> new ConfigPontosCorridos(1, 1, 1, 0, true, CRITERIOS)).doesNotThrowAnyException();
        assertThatCode(() -> new ConfigPontosCorridos(1, 3, 0, 0, true, CRITERIOS)).doesNotThrowAnyException();
        assertThatCode(() -> new ConfigPontosCorridos(1, 2, 1, -1, true, CRITERIOS)).doesNotThrowAnyException();
    }

    @Test
    void recusaCriteriosNulosOuVazios() {
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, null))
                .isInstanceOf(ConfigInvalidaException.class);
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, List.of()))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("critério");
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, Arrays.asList(PONTOS, null)))
                .isInstanceOf(ConfigInvalidaException.class);
    }

    @Test
    void recusaCriteriosRepetidos() {
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, List.of(PONTOS, SALDO, SALDO)))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("repetir");
    }

    @Test
    void recusaPontosForaDaPrimeiraPosicao() {
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, List.of(VITORIAS, PONTOS)))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("PONTOS");
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, List.of(SALDO)))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("PONTOS");
    }

    @Test
    void recusaSorteioForaDaUltimaPosicao() {
        assertThatThrownBy(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, List.of(PONTOS, SORTEIO, SALDO)))
                .isInstanceOf(ConfigInvalidaException.class)
                .hasMessageContaining("SORTEIO");
    }

    @Test
    void aceitaSorteioComoUltimoOuAusente() {
        assertThatCode(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, List.of(PONTOS, SORTEIO)))
                .doesNotThrowAnyException();
        assertThatCode(() -> new ConfigPontosCorridos(1, 3, 1, 0, true, List.of(PONTOS))).doesNotThrowAnyException();
    }

    @Test
    void copiaOsCriteriosEHaImutabilidade() {
        List<CriterioDesempate> original = new ArrayList<>(List.of(PONTOS, SALDO));
        ConfigPontosCorridos config = new ConfigPontosCorridos(1, 3, 1, 0, true, original);

        original.add(PRO);

        assertThat(config.criterios()).containsExactly(PONTOS, SALDO);
        assertThatThrownBy(() -> config.criterios().add(PRO)).isInstanceOf(UnsupportedOperationException.class);
    }
}
