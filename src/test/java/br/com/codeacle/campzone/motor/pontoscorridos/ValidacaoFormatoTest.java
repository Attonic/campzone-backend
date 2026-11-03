package br.com.codeacle.campzone.motor.pontoscorridos;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;
import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesDeTeste;
import br.com.codeacle.campzone.motor.ParticipantesInvalidosException;
import br.com.codeacle.campzone.motor.ResultadoInvalidoException;
import br.com.codeacle.campzone.motor.ResultadoJogo;

/**
 * Validações de entrada do formato pontos corridos (participantes, config e resultado). Placar negativo e mandante
 * igual ao visitante são barrados na criação do {@code ResultadoJogo}, antes de chegar ao formato, e por isso são
 * testados no {@code ResultadoJogoTest}.
 */
class ValidacaoFormatoTest {

    private final PontosCorridosFormato formato = new PontosCorridosFormato();
    private final ConfigPontosCorridos config = ConfigPontosCorridos.padrao(1);
    private final ConfigPontosCorridos semEmpate = new ConfigPontosCorridos(1, 3, 1, 0, false,
            List.of(CriterioDesempate.PONTOS));

    // ---------------------------------------------------------------- gerar: participantes e config

    @Test
    void gerarRecusaMenosDeDoisParticipantes() {
        assertThatThrownBy(() -> formato.gerar(config, ParticipantesDeTeste.com(1), 1L))
                .isInstanceOf(ParticipantesInvalidosException.class);
        assertThatThrownBy(() -> formato.gerar(config, List.of(), 1L))
                .isInstanceOf(ParticipantesInvalidosException.class);
    }

    @Test
    void gerarRecusaListaNulaOuComItemNulo() {
        assertThatThrownBy(() -> formato.gerar(config, null, 1L)).isInstanceOf(ParticipantesInvalidosException.class);
        assertThatThrownBy(() -> formato.gerar(config, Arrays.asList(new ParticipanteMotor(1, "A", null), null), 1L))
                .isInstanceOf(ParticipantesInvalidosException.class);
    }

    @Test
    void gerarRecusaIdsRepetidos() {
        assertThatThrownBy(() -> formato.gerar(config, ParticipantesDeTeste.comIds(1, 2, 2), 1L))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("repetido");
    }

    @Test
    void gerarRecusaNomeVazio() {
        assertThatThrownBy(() -> formato.gerar(config,
                List.of(new ParticipanteMotor(1, "A", null), new ParticipanteMotor(2, " ", null)), 1L))
                .isInstanceOf(ParticipantesInvalidosException.class);
    }

    @Test
    void gerarRecusaConfigNula() {
        assertThatThrownBy(() -> formato.gerar(null, ParticipantesDeTeste.com(4), 1L))
                .isInstanceOf(ConfigInvalidaException.class);
    }

    // ---------------------------------------------------------------- validarResultado

    @Test
    void aceitaVitoriaDeQualquerLadoEEmpateQuandoPermitido() {
        assertThatCode(() -> formato.validarResultado(config, new ResultadoJogo(1, 2, 2, 1))).doesNotThrowAnyException();
        assertThatCode(() -> formato.validarResultado(config, new ResultadoJogo(1, 2, 0, 3))).doesNotThrowAnyException();
        assertThatCode(() -> formato.validarResultado(config, new ResultadoJogo(1, 2, 1, 1))).doesNotThrowAnyException();
        assertThatCode(() -> formato.validarResultado(config, new ResultadoJogo(1, 2, 0, 0))).doesNotThrowAnyException();
    }

    @Test
    void recusaEmpateQuandoNaoPermitidoComMensagemClara() {
        assertThatThrownBy(() -> formato.validarResultado(semEmpate, new ResultadoJogo(1, 2, 2, 2)))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("não permite empate")
                .hasMessageContaining("1 x 2")
                .hasMessageContaining("2 x 2");
        assertThatThrownBy(() -> formato.validarResultado(semEmpate, new ResultadoJogo(3, 4, 0, 0)))
                .isInstanceOf(ResultadoInvalidoException.class);
    }

    @Test
    void semEmpatePermitidoAindaAceitaVitoria() {
        assertThatCode(() -> formato.validarResultado(semEmpate, new ResultadoJogo(1, 2, 1, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void validarResultadoRecusaResultadoOuConfigNulos() {
        assertThatThrownBy(() -> formato.validarResultado(config, null))
                .isInstanceOf(ResultadoInvalidoException.class);
        assertThatThrownBy(() -> formato.validarResultado(null, new ResultadoJogo(1, 2, 1, 0)))
                .isInstanceOf(ConfigInvalidaException.class);
    }
}
