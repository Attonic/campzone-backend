package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PRORROGACAO;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.SORTEIO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.FORMATO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.SEED;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.calcular;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comPenaltis;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comProrrogacao;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comSeeding;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idaEVolta;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogoUnico;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.participantes;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.placar;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.serie;
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

/** Validações de entrada do formato mata-mata: participantes, config, resultado sem contexto e no contexto da chave. */
class ValidacaoMataMataTest {

    private final ConfigMataMata config = jogoUnico(PENALTIS);

    private void rejeita(ConfigMataMata config, int n, String trecho, ResultadoMataMata... resultados) {
        assertThatThrownBy(() -> calcular(config, n, resultados)).isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining(trecho);
    }

    // ---------------------------------------------------------------- participantes e config

    @Test
    void gerarRecusaMenosDeDoisParticipantes() {
        assertThatThrownBy(() -> FORMATO.gerar(config, participantes(1), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class);
        assertThatThrownBy(() -> FORMATO.gerar(config, List.of(), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class);
    }

    @Test
    void gerarRecusaListaNulaItemNuloIdsRepetidosENomeVazio() {
        assertThatThrownBy(() -> FORMATO.gerar(config, null, SEED)).isInstanceOf(ParticipantesInvalidosException.class);
        assertThatThrownBy(() -> FORMATO.gerar(config, Arrays.asList(new ParticipanteMotor(1, "A", 1), null), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class);
        assertThatThrownBy(() -> FORMATO.gerar(config, ParticipantesDeTeste.comIds(1, 2, 2), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class).hasMessageContaining("repetido");
        assertThatThrownBy(() -> FORMATO.gerar(config,
                List.of(new ParticipanteMotor(1, "A", 1), new ParticipanteMotor(2, " ", 2)), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class);
    }

    @Test
    void gerarRecusaConfigNula() {
        assertThatThrownBy(() -> FORMATO.gerar(null, participantes(4), SEED)).isInstanceOf(ConfigInvalidaException.class);
        assertThatThrownBy(() -> FORMATO.calcular(null, participantes(4), List.of(), SEED))
                .isInstanceOf(ConfigInvalidaException.class);
    }

    @Test
    void gerarRecusaSeedsQueNaoServemAoSeeding() {
        // MANUAL exige a seed de todos: aqui os participantes não têm seed nenhuma
        assertThatThrownBy(() -> FORMATO.gerar(config, ParticipantesDeTeste.com(4), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class).hasMessageContaining("MANUAL");
        assertThatThrownBy(() -> FORMATO.gerar(comSeeding(config, Seeding.CABECAS_DE_CHAVE),
                ParticipantesDeTeste.comSeeds(1, 3, null, null), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class).hasMessageContaining("sem buracos");
    }

    @Test
    void calcularRecusaListaDeResultadosNulaOuComItemNulo() {
        assertThatThrownBy(() -> FORMATO.calcular(config, participantes(4), null, SEED))
                .isInstanceOf(ResultadoInvalidoException.class);
        assertThatThrownBy(() -> FORMATO.calcular(config, participantes(4),
                Arrays.asList(placar(1, 1, 1, 1, 0), null), SEED)).isInstanceOf(ResultadoInvalidoException.class);
    }

    // ---------------------------------------------------------------- validarResultado, sem contexto

    @Test
    void validarResultadoAceitaOsDadosQueAConfigPrevê() {
        ConfigMataMata completa = jogoUnico(PRORROGACAO, PENALTIS);

        assertThatCode(() -> FORMATO.validarResultado(completa, placar(1, 1, 1, 2, 1))).doesNotThrowAnyException();
        assertThatCode(() -> FORMATO.validarResultado(completa, comProrrogacao(1, 1, 1, 1, 1, 1, 1)))
                .doesNotThrowAnyException();
        assertThatCode(() -> FORMATO.validarResultado(completa, comPenaltis(1, 1, 1, 1, 1, 4, 3)))
                .doesNotThrowAnyException();
    }

    @Test
    void validarResultadoRecusaProrrogacaoQuandoOCriterioNaoEstaNaConfig() {
        assertThatThrownBy(() -> FORMATO.validarResultado(jogoUnico(PENALTIS), comProrrogacao(1, 1, 1, 1, 1, 1, 0)))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("PRORROGACAO");
    }

    @Test
    void validarResultadoRecusaPenaltisQuandoOCriterioNaoEstaNaConfig() {
        assertThatThrownBy(() -> FORMATO.validarResultado(jogoUnico(SORTEIO), comPenaltis(1, 1, 1, 1, 1, 4, 3)))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("PENALTIS");
    }

    @Test
    void validarResultadoRecusaPenaltisEmpatados() {
        assertThatThrownBy(() -> FORMATO.validarResultado(config, comPenaltis(1, 1, 1, 1, 1, 3, 3)))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("empatados");
    }

    @Test
    void validarResultadoRecusaResultadoOuConfigNulos() {
        assertThatThrownBy(() -> FORMATO.validarResultado(config, null)).isInstanceOf(ResultadoInvalidoException.class);
        assertThatThrownBy(() -> FORMATO.validarResultado(null, placar(1, 1, 1, 1, 0)))
                .isInstanceOf(ConfigInvalidaException.class);
    }

    @Test
    void validarResultadoNaoSabeSeOEmpateEValido() {
        // sem contexto, 1 x 1 sem dados passa; só o calcular sabe que o jogo decisivo não aceita empate sem desempate
        assertThatCode(() -> FORMATO.validarResultado(config, placar(1, 1, 1, 1, 1))).doesNotThrowAnyException();
        rejeita(config, 2, "informe os pênaltis", placar(1, 1, 1, 1, 1));
    }

    // ---------------------------------------------------------------- jogo que não aceita resultado

    @Test
    void recusaJogoInexistente() {
        rejeita(config, 2, "não existe", placar(9, 1, 1, 1, 0));
        rejeita(config, 2, "não existe", placar(1, 2, 1, 1, 0));
        rejeita(config, 2, "não existe", placar(1, 1, 2, 1, 0));
        rejeita(config, 2, "não existe", placar(1, 1, 0, 1, 0));
        rejeita(config, 2, "não existe", placar(0, 1, 1, 1, 0));
        rejeita(serie(3, PENALTIS), 2, "não existe", placar(1, 1, 4, 1, 0));
    }

    @Test
    void recusaResultadoDeConfrontoDeBye() {
        // n = 3: a seed 1 tem bye na semifinal 1, que não tem jogos
        rejeita(config, 3, "não existe", placar(1, 1, 1, 1, 0));
    }

    @Test
    void recusaJogoQueAindaAguardaParticipantes() {
        // n = 4: a final só abre quando as duas semifinais têm vencedor
        rejeita(config, 4, "aguarda os participantes", placar(2, 1, 1, 1, 0));
        rejeita(config, 4, "aguarda os participantes", placar(1, 1, 1, 1, 0), placar(2, 1, 1, 1, 0));
    }

    @Test
    void recusaJogoDesnecessarioNaSerie() {
        // melhor de 3: A vence os jogos 1 (em casa) e 2 (fora)
        rejeita(serie(3, PENALTIS), 2, "desnecessário", placar(1, 1, 1, 1, 0), placar(1, 1, 2, 0, 1),
                placar(1, 1, 3, 1, 0));
        // melhor de 5, varrida de 3 a 0: o jogo 4 já é desnecessário
        rejeita(serie(5, PENALTIS), 2, "desnecessário", placar(1, 1, 1, 1, 0), placar(1, 1, 2, 0, 1),
                placar(1, 1, 3, 1, 0), placar(1, 1, 4, 0, 1));
    }

    @Test
    void recusaResultadoDuplicado() {
        rejeita(config, 2, "duplicado", placar(1, 1, 1, 1, 0), placar(1, 1, 1, 0, 1));
        rejeita(config, 2, "duplicado", placar(1, 1, 1, 1, 0), placar(1, 1, 1, 1, 0));
        rejeita(serie(3, PENALTIS), 2, "duplicado", placar(1, 1, 1, 1, 0), placar(1, 1, 2, 0, 1),
                placar(1, 1, 2, 0, 1));
    }

    // ---------------------------------------------------------------- jogos lançados fora de ordem

    @Test
    void recusaAVoltaSemAIda() {
        rejeita(idaEVolta(PENALTIS), 2, "jogo de ida", placar(1, 1, 2, 1, 0));
    }

    @Test
    void recusaJogoDaSerieSemOsAnteriores() {
        rejeita(serie(3, PENALTIS), 2, "jogos anteriores", placar(1, 1, 2, 1, 0));
        rejeita(serie(5, PENALTIS), 2, "jogos anteriores", placar(1, 1, 1, 1, 0), placar(1, 1, 3, 1, 0));
    }

    @Test
    void aOrdemDaListaNaoImportaParaAOrdemDosJogos() {
        // jogo 2 antes do jogo 1 na lista: vale, porque o que conta é o número do jogo
        assertThatCode(() -> calcular(serie(3, PENALTIS), 2, placar(1, 1, 2, 0, 1), placar(1, 1, 1, 1, 0)))
                .doesNotThrowAnyException();
    }
}
