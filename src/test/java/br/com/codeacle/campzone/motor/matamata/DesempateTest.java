package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.GOLS_FORA;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PRORROGACAO;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.SORTEIO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.FORMATO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.SEED;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.calcular;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comPenaltis;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comProrrogacao;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.completo;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.confronto;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idDaSeed;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idaEVolta;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogoUnico;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.participantes;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.placar;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.serie;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.situacoes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.motor.ResultadoInvalidoException;

/**
 * Desempates do mata-mata, isolados e combinados, e os resultados inválidos de desempate. Os confrontos são a final de
 * uma chave de 2 participantes: A é a seed 1 (id 10) e B é a seed 2 (id 17).
 */
class DesempateTest {

    private static final long A = idDaSeed(1);
    private static final long B = idDaSeed(2);

    private static long vencedor(ConfigMataMata config, ResultadoMataMata... resultados) {
        EstadoMataMata estado = calcular(config, 2, resultados);
        assertThat(estado.campeaoId()).as("campeão").isNotNull();
        assertThat(confronto(estado, 1, 1).vencedorId()).isEqualTo(estado.campeaoId());
        return estado.campeaoId();
    }

    private static void rejeita(ConfigMataMata config, String trecho, ResultadoMataMata... resultados) {
        assertThatThrownBy(() -> calcular(config, 2, resultados)).isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining(trecho);
    }

    // ---------------------------------------------------------------- GOLS_FORA isolado

    @Test
    void golsForaDaVitoriaAQuemMarcouMaisComoVisitante() {
        ConfigMataMata config = idaEVolta(GOLS_FORA, PENALTIS);

        // ida: B 1 x 2 A (A faz 2 fora); volta: A 0 x 1 B (B faz 1 fora). Agregado 2 x 2; fora: A 2, B 1
        assertThat(vencedor(config, placar(1, 1, 1, 1, 2), placar(1, 1, 2, 0, 1))).isEqualTo(A);
        // ida: B 0 x 1 A (A faz 1 fora); volta: A 1 x 2 B (B faz 2 fora). Agregado 2 x 2; fora: A 1, B 2
        assertThat(vencedor(config, placar(1, 1, 1, 0, 1), placar(1, 1, 2, 1, 2))).isEqualTo(B);
    }

    @Test
    void golsForaNaoEntramQuandoOAgregadoJaDecide() {
        // ida: B 5 x 2 A; volta: A 1 x 0 B. Agregado A 3 x 5 B. Os gols fora (A 2, B 0) favoreceriam o A, mas o
        // agregado manda e nenhum dado de desempate é pedido
        assertThat(vencedor(idaEVolta(GOLS_FORA, PENALTIS), placar(1, 1, 1, 5, 2), placar(1, 1, 2, 1, 0))).isEqualTo(B);
    }

    // ---------------------------------------------------------------- PRORROGACAO isolada

    @Test
    void prorrogacaoDaVitoriaAQuemMarcouMaisNela() {
        ConfigMataMata config = jogoUnico(PRORROGACAO, SORTEIO);

        assertThat(vencedor(config, comProrrogacao(1, 1, 1, 1, 1, 1, 0))).isEqualTo(A);
        assertThat(vencedor(config, comProrrogacao(1, 1, 1, 1, 1, 0, 2))).isEqualTo(B);
    }

    @Test
    void prorrogacaoValeSoOsGolsDelaENaoOAcumuladoComOTempoNormal() {
        // 3 x 3 no tempo normal; prorrogação 0 x 1 -> B vence, ainda que o mandante tenha marcado mais no total até aqui
        assertThat(vencedor(jogoUnico(PRORROGACAO, SORTEIO), comProrrogacao(1, 1, 1, 3, 3, 0, 1))).isEqualTo(B);
    }

    @Test
    void prorrogacaoNaIdaEVoltaFicaNoJogoDois() {
        ConfigMataMata config = idaEVolta(PRORROGACAO, PENALTIS);

        // agregado 1 x 1; no jogo 2 (A em casa) a prorrogação termina 0 x 1 -> B
        assertThat(vencedor(config, placar(1, 1, 1, 0, 0), comProrrogacao(1, 1, 2, 1, 1, 0, 1))).isEqualTo(B);
    }

    @Test
    void prorrogacaoNaSerieDecideSoOJogoEmpatado() {
        ConfigMataMata config = serie(3, PRORROGACAO, PENALTIS);

        // jogo 1 (A em casa) empata e a prorrogação dá a vitória a A; jogo 2 (B em casa) A vence fora: 2 x 0
        EstadoMataMata estado = calcular(config, 2, comProrrogacao(1, 1, 1, 1, 1, 1, 0), placar(1, 1, 2, 0, 1));
        assertThat(estado.campeaoId()).isEqualTo(A);
        assertThat(situacoes(confronto(estado, 1, 1))).containsExactly(SituacaoJogo.DISPUTADO, SituacaoJogo.DISPUTADO,
                SituacaoJogo.DESNECESSARIO);
    }

    @Test
    void prorrogacaoDoVisitanteNaSerieValeParaOVisitanteDaqueleJogo() {
        ConfigMataMata config = serie(3, PRORROGACAO, PENALTIS);

        // jogo 2: B em casa e A visitante; empate no tempo normal e prorrogação 0 x 2 (visitante = A) -> A vence o jogo
        EstadoMataMata estado = calcular(config, 2, placar(1, 1, 1, 1, 0), comProrrogacao(1, 1, 2, 1, 1, 0, 2));
        assertThat(estado.campeaoId()).isEqualTo(A);
    }

    // ---------------------------------------------------------------- PENALTIS isolado

    @Test
    void penaltisDaVitoriaAQuemFezMais() {
        ConfigMataMata config = jogoUnico(PENALTIS);

        assertThat(vencedor(config, comPenaltis(1, 1, 1, 0, 0, 5, 4))).isEqualTo(A);
        assertThat(vencedor(config, comPenaltis(1, 1, 1, 2, 2, 3, 4))).isEqualTo(B);
    }

    @Test
    void penaltisNaIdaEVoltaSaoDoJogoDois() {
        // agregado 2 x 2; nos pênaltis do jogo 2 (A é o mandante) vence B por 3 x 4
        assertThat(vencedor(idaEVolta(PENALTIS), placar(1, 1, 1, 1, 1), comPenaltis(1, 1, 2, 1, 1, 3, 4)))
                .isEqualTo(B);
    }

    @Test
    void penaltisNaSerieDecidemSoOJogoEmpatado() {
        ConfigMataMata config = serie(3, PENALTIS);

        // jogo 1: A em casa 0 x 0 e A vence nos pênaltis; jogo 2: B em casa, 1 x 1 e pênaltis 2 x 3 (B) ; jogo 3: A 1 x 0
        EstadoMataMata estado = calcular(config, 2, comPenaltis(1, 1, 1, 0, 0, 4, 3), comPenaltis(1, 1, 2, 1, 1, 3, 2),
                placar(1, 1, 3, 1, 0));
        assertThat(estado.campeaoId()).isEqualTo(A);
        assertThat(confronto(estado, 1, 1).jogos()).extracting(JogoEstado::situacao)
                .containsOnly(SituacaoJogo.DISPUTADO);
    }

    // ---------------------------------------------------------------- SORTEIO isolado

    @Test
    void sorteioSegueAFormulaDoAdendoEEReprodutivel() {
        ConfigMataMata config = jogoUnico(SORTEIO);

        for (long seed : new long[] { 0, 1, 42, 20260, -7 }) {
            boolean favoreceA = new Random(seed + 1_000_003L * 1 + 1).nextBoolean();
            EstadoMataMata estado = FORMATO.calcular(config, participantes(2), List.of(placar(1, 1, 1, 1, 1)), seed);
            EstadoMataMata outraVez = FORMATO.calcular(config, participantes(2), List.of(placar(1, 1, 1, 1, 1)), seed);

            assertThat(estado.campeaoId()).as("seed %d", seed).isEqualTo(favoreceA ? A : B);
            assertThat(outraVez).isEqualTo(estado);
        }
    }

    @Test
    void sorteioPodeFavorecerCadaLadoConformeASeed() {
        // seeds de campeonato espalhadas (a camada de aplicação sorteia a seed como um long qualquer)
        Random gerador = new Random(1234);
        Set<Long> campeoes = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            campeoes.add(FORMATO.calcular(jogoUnico(SORTEIO), participantes(2), List.of(placar(1, 1, 1, 0, 0)),
                    gerador.nextLong()).campeaoId());
        }

        assertThat(campeoes).containsExactlyInAnyOrder(A, B);
    }

    @Test
    void sorteioNaoPrecisaDeDadosNoResultadoENaoMudaComOutrosResultados() {
        ConfigMataMata config = idaEVolta(SORTEIO);

        // agregado 1 x 1, sem prorrogação nem pênaltis: o sorteio decide
        EstadoMataMata estado = calcular(config, 2, placar(1, 1, 1, 0, 1), placar(1, 1, 2, 0, 1));
        long esperado = new Random(SEED + 1_000_003L + 1).nextBoolean() ? A : B;

        assertThat(estado.campeaoId()).isEqualTo(esperado);
    }

    // ---------------------------------------------------------------- combinações

    @Test
    void golsForaEmpatadoVaiParaProrrogacaoEDepoisParaPenaltis() {
        ConfigMataMata config = idaEVolta(GOLS_FORA, PRORROGACAO, PENALTIS);
        // ida: B 1 x 1 A; volta: A 1 x 1 B. Agregado 2 x 2; fora 1 x 1 -> segue para a prorrogação do jogo 2
        ResultadoMataMata ida = placar(1, 1, 1, 1, 1);

        // sem prorrogação: pede
        rejeita(config, "informe a prorrogação", ida, placar(1, 1, 2, 1, 1));
        // prorrogação empatada 0 x 0: pede os pênaltis
        rejeita(config, "informe os pênaltis", ida, comProrrogacao(1, 1, 2, 1, 1, 0, 0));
        // prorrogação decide
        assertThat(vencedor(config, ida, comProrrogacao(1, 1, 2, 1, 1, 2, 1))).isEqualTo(A);
        // prorrogação empatada e pênaltis decidem
        assertThat(vencedor(config, ida, completo(1, 1, 2, 1, 1, 1, 1, 3, 5))).isEqualTo(B);
    }

    @Test
    void golsForaQueDecideDispensaProrrogacaoEPenaltis() {
        ConfigMataMata config = idaEVolta(GOLS_FORA, PRORROGACAO, PENALTIS);

        // agregado 2 x 2 com gols fora A 2 x 0 B: decidido sem mais dados
        assertThat(vencedor(config, placar(1, 1, 1, 0, 2), placar(1, 1, 2, 2, 0))).isEqualTo(A);
    }

    @Test
    void prorrogacaoEmpatadaVaiParaOSorteioSemPenaltis() {
        ConfigMataMata config = jogoUnico(PRORROGACAO, SORTEIO);
        long esperado = new Random(SEED + 1_000_003L + 1).nextBoolean() ? A : B;

        assertThat(vencedor(config, comProrrogacao(1, 1, 1, 2, 2, 1, 1))).isEqualTo(esperado);
    }

    @Test
    void ordemConfiguradaPrevalece() {
        // com PENALTIS antes de PRORROGACAO, os pênaltis decidem e a prorrogação nunca é pedida nem aceita
        ConfigMataMata config = jogoUnico(PENALTIS, PRORROGACAO);

        assertThat(vencedor(config, comPenaltis(1, 1, 1, 1, 1, 4, 3))).isEqualTo(A);
        rejeita(config, "informe os pênaltis", placar(1, 1, 1, 1, 1));
        rejeita(config, "não precisa da prorrogação", completo(1, 1, 1, 1, 1, 1, 0, 4, 3));
    }

    @Test
    void prorrogacaoAntesDosGolsForaTambemRespeitaAOrdemConfigurada() {
        ConfigMataMata config = idaEVolta(PRORROGACAO, GOLS_FORA, PENALTIS);

        // ida: B 1 x 2 A; volta: A 0 x 1 B. Agregado 2 x 2; a prorrogação (primeiro critério) empata 0 x 0, então os
        // gols fora decidem: A fez 2 fora e B fez 1
        assertThat(vencedor(config, placar(1, 1, 1, 1, 2), comProrrogacao(1, 1, 2, 0, 1, 0, 0))).isEqualTo(A);
    }

    // ---------------------------------------------------------------- resultados inválidos de desempate

    @Test
    void empateSemPrimeiroDadoExigidoPedeAProrrogacao() {
        // exemplo da spec: PRORROGACAO, PENALTIS e jogo único 1 x 1 sem prorrogação
        rejeita(jogoUnico(PRORROGACAO, PENALTIS), "informe a prorrogação", placar(1, 1, 1, 1, 1));
    }

    @Test
    void empateComProrrogacaoEmpatadaSemPenaltisPedeOsPenaltis() {
        rejeita(jogoUnico(PRORROGACAO, PENALTIS), "informe os pênaltis", comProrrogacao(1, 1, 1, 1, 1, 0, 0));
    }

    @Test
    void empateSemPenaltisNaConfigSoComPenaltisPedeOsPenaltis() {
        rejeita(jogoUnico(PENALTIS), "informe os pênaltis", placar(1, 1, 1, 0, 0));
    }

    @Test
    void penaltisEmpatadosSaoInvalidos() {
        rejeita(jogoUnico(PENALTIS), "empatados", comPenaltis(1, 1, 1, 1, 1, 3, 3));
    }

    @Test
    void prorrogacaoOuPenaltisNumJogoQueJaTemVencedorSaoInvalidos() {
        rejeita(jogoUnico(PRORROGACAO, PENALTIS), "não precisa da prorrogação", comProrrogacao(1, 1, 1, 2, 1, 1, 0));
        rejeita(jogoUnico(PRORROGACAO, PENALTIS), "não precisa dos pênaltis", comPenaltis(1, 1, 1, 2, 1, 3, 2));
    }

    @Test
    void penaltisSobrandoDepoisQueAProrrogacaoDecidiuSaoInvalidos() {
        rejeita(jogoUnico(PRORROGACAO, PENALTIS), "não precisa dos pênaltis", completo(1, 1, 1, 1, 1, 2, 1, 4, 3));
    }

    @Test
    void prorrogacaoEPenaltisNaIdaDaIdaEVoltaSaoInvalidos() {
        ConfigMataMata config = idaEVolta(PRORROGACAO, PENALTIS);

        rejeita(config, "jogo 1", comProrrogacao(1, 1, 1, 1, 1, 1, 0));
        rejeita(config, "jogo 1", comPenaltis(1, 1, 1, 1, 1, 4, 3));
    }

    @Test
    void prorrogacaoEPenaltisNoJogoDoisQuandoOAgregadoJaDecideSaoInvalidos() {
        ConfigMataMata config = idaEVolta(PRORROGACAO, PENALTIS);

        rejeita(config, "não precisa da prorrogação", placar(1, 1, 1, 0, 2), comProrrogacao(1, 1, 2, 1, 0, 1, 0));
    }

    @Test
    void golsForaQueDecidiuDispensaAProrrogacaoInformada() {
        ConfigMataMata config = idaEVolta(GOLS_FORA, PRORROGACAO, PENALTIS);

        rejeita(config, "não precisa da prorrogação", placar(1, 1, 1, 0, 2), comProrrogacao(1, 1, 2, 2, 0, 1, 0));
    }

    @Test
    void serieExigeOsDadosDoJogoEmpatado() {
        rejeita(serie(3, PRORROGACAO, PENALTIS), "informe a prorrogação", placar(1, 1, 1, 1, 1));
        rejeita(serie(3, PENALTIS), "informe os pênaltis", placar(1, 1, 1, 0, 1), placar(1, 1, 2, 2, 2));
    }

    @Test
    void serieRecusaDadosDeDesempateNumJogoQueTemVencedor() {
        rejeita(serie(3, PRORROGACAO, PENALTIS), "não precisa da prorrogação", comProrrogacao(1, 1, 1, 2, 1, 1, 0));
    }

    @Test
    void resultadoComPrimeiroCriterioNaoUsadoAindaPedeOQueFalta() {
        // GOLS_FORA desempata na ida e volta, mas aqui o agregado é 0 x 0 e não há gols fora: segue para pênaltis
        ConfigMataMata config = idaEVolta(GOLS_FORA, PENALTIS);

        rejeita(config, "informe os pênaltis", placar(1, 1, 1, 0, 0), placar(1, 1, 2, 0, 0));
        List<ResultadoMataMata> completo = new ArrayList<>(List.of(placar(1, 1, 1, 0, 0), comPenaltis(1, 1, 2, 0, 0, 5, 4)));
        assertThat(FORMATO.calcular(config, participantes(2), completo, SEED).campeaoId()).isEqualTo(A);
    }
}
