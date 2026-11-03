package br.com.codeacle.campzone.motor.pontoscorridos;

import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.CONFRONTO_DIRETO;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.PONTOS;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.PRO;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.SALDO;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.SORTEIO;
import static br.com.codeacle.campzone.motor.pontoscorridos.CriterioDesempate.VITORIAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;
import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesDeTeste;
import br.com.codeacle.campzone.motor.ParticipantesInvalidosException;
import br.com.codeacle.campzone.motor.ResultadoInvalidoException;
import br.com.codeacle.campzone.motor.ResultadoJogo;

/** Cálculo da classificação: estatísticas, cada critério isolado, combinações, sorteio e posições divididas. */
class ClassificacaoTest {

    private static final long SEED = 42L;

    private final PontosCorridosFormato formato = new PontosCorridosFormato();
    private final List<ParticipanteMotor> quatro = ParticipantesDeTeste.comIds(1, 2, 3, 4);

    private static ResultadoJogo r(long mandante, long visitante, int placarMandante, int placarVisitante) {
        return new ResultadoJogo(mandante, visitante, placarMandante, placarVisitante);
    }

    private static ConfigPontosCorridos config(CriterioDesempate... criterios) {
        return new ConfigPontosCorridos(1, 3, 1, 0, true, List.of(criterios));
    }

    private Classificacao calcular(ConfigPontosCorridos config, ResultadoJogo... resultados) {
        return formato.calcular(config, quatro, List.of(resultados), SEED);
    }

    private static List<Long> ids(Classificacao c) {
        return c.linhas().stream().map(LinhaClassificacao::participanteId).toList();
    }

    private static List<Integer> posicoes(Classificacao c) {
        return c.linhas().stream().map(LinhaClassificacao::posicao).toList();
    }

    private static List<Integer> pontos(Classificacao c) {
        return c.linhas().stream().map(LinhaClassificacao::pontos).toList();
    }

    // ---------------------------------------------------------------- estatísticas

    @Test
    void semJogosTodosApareceComZeroEDividemAPrimeiraPosicao() {
        Classificacao c = calcular(config(PONTOS));

        assertThat(c.linhas()).hasSize(4);
        assertThat(ids(c)).containsExactly(1L, 2L, 3L, 4L);
        assertThat(posicoes(c)).containsOnly(1);
        assertThat(c.linhas()).allSatisfy(l -> assertThat(l).extracting(LinhaClassificacao::pontos,
                LinhaClassificacao::jogos, LinhaClassificacao::vitorias, LinhaClassificacao::empates,
                LinhaClassificacao::derrotas, LinhaClassificacao::golsPro, LinhaClassificacao::golsContra,
                LinhaClassificacao::saldo).containsOnly(0));
    }

    @Test
    void acumulaPontosJogosVEDGolsESaldo() {
        Classificacao c = calcular(config(PONTOS, CONFRONTO_DIRETO, SALDO), cicloComTresEmpatados());

        assertThat(c.linhas()).containsExactly(
                new LinhaClassificacao(1, 3, 5, 4, 1, 2, 1, 1, 3, -2),
                new LinhaClassificacao(2, 2, 5, 4, 1, 2, 1, 3, 1, 2),
                new LinhaClassificacao(3, 1, 5, 4, 1, 2, 1, 1, 1, 0),
                new LinhaClassificacao(4, 4, 2, 2, 0, 2, 0, 0, 0, 0));
    }

    private static ResultadoJogo[] cicloComTresEmpatados() {
        return new ResultadoJogo[] {
                r(1, 2, 1, 0), // 1 vence 2
                r(1, 3, 0, 0), // 1 e 3 empatam
                r(3, 1, 1, 0), // 3 vence 1
                r(2, 3, 3, 0), // 2 vence 3 por 3 a 0
                r(3, 2, 0, 0), // 3 e 2 empatam
                r(1, 4, 0, 0), // 1 e 4 empatam
                r(2, 4, 0, 0) // 2 e 4 empatam
        };
    }

    // ---------------------------------------------------------------- cada critério isolado

    @Test
    void pontosOrdenamDecrescente() {
        Classificacao c = formato.calcular(config(PONTOS), ParticipantesDeTeste.comIds(1, 2, 3),
                List.of(r(1, 2, 0, 1)), SEED);

        assertThat(ids(c)).containsExactly(2L, 1L, 3L);
        assertThat(posicoes(c)).containsExactly(1, 2, 2);
    }

    @Test
    void vitoriasDecidemEmpateDePontos() {
        ResultadoJogo[] jogos = { r(1, 2, 0, 0), r(2, 1, 0, 0), r(1, 2, 0, 0), r(3, 4, 1, 0) };

        Classificacao semCriterio = calcular(config(PONTOS), jogos);
        assertThat(ids(semCriterio)).containsExactly(1L, 2L, 3L, 4L);
        assertThat(posicoes(semCriterio)).containsExactly(1, 1, 1, 4);

        Classificacao c = calcular(config(PONTOS, VITORIAS), jogos);
        assertThat(pontos(c)).containsExactly(3, 3, 3, 0);
        assertThat(ids(c)).containsExactly(3L, 1L, 2L, 4L);
        assertThat(posicoes(c)).containsExactly(1, 2, 2, 4);
    }

    @Test
    void saldoDecideEmpateDePontos() {
        ResultadoJogo[] jogos = { r(1, 4, 1, 0), r(2, 3, 3, 0) };

        Classificacao semCriterio = calcular(config(PONTOS), jogos);
        assertThat(ids(semCriterio)).containsExactly(1L, 2L, 3L, 4L);
        assertThat(posicoes(semCriterio)).containsExactly(1, 1, 3, 3);

        Classificacao c = calcular(config(PONTOS, SALDO), jogos);
        assertThat(ids(c)).containsExactly(2L, 1L, 4L, 3L);
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 4);
    }

    @Test
    void proDecideQuandoPontosVitoriasESaldoEmpatam() {
        ResultadoJogo[] jogos = { r(1, 2, 1, 0), r(3, 4, 3, 2) };

        Classificacao porSaldo = calcular(config(PONTOS, VITORIAS, SALDO), jogos);
        assertThat(ids(porSaldo)).containsExactly(1L, 3L, 2L, 4L);
        assertThat(posicoes(porSaldo)).containsExactly(1, 1, 3, 3);

        Classificacao c = calcular(config(PONTOS, VITORIAS, SALDO, PRO), jogos);
        assertThat(ids(c)).containsExactly(3L, 1L, 4L, 2L);
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 4);
    }

    @Test
    void confrontoDiretoDecideEmpateDePontos() {
        ResultadoJogo[] jogos = { r(2, 1, 1, 0), r(1, 3, 5, 0) };

        Classificacao semCriterio = calcular(config(PONTOS), jogos);
        assertThat(ids(semCriterio)).containsExactly(1L, 2L, 3L, 4L);
        assertThat(posicoes(semCriterio)).containsExactly(1, 1, 3, 3);

        Classificacao c = calcular(config(PONTOS, CONFRONTO_DIRETO), jogos);
        assertThat(ids(c)).containsExactly(2L, 1L, 3L, 4L);
        // 3 e 4 não se enfrentaram: seguem empatados e dividem a posição
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 3);
    }

    @Test
    void confrontoDiretoSomaOsJogosDeIdaEVolta() {
        // 1 e 2 chegam a 7 pontos. No confronto direto: 1 venceu em casa e empatou fora (4 x 1)
        ResultadoJogo[] jogos = { r(1, 2, 2, 0), r(2, 1, 1, 1), r(2, 3, 5, 0), r(3, 2, 0, 5), r(1, 3, 1, 0) };

        Classificacao porSaldo = calcular(config(PONTOS, SALDO, CONFRONTO_DIRETO), jogos);
        assertThat(pontos(porSaldo)).containsExactly(7, 7, 0, 0);
        assertThat(ids(porSaldo)).containsExactly(2L, 1L, 4L, 3L);

        Classificacao porConfronto = calcular(config(PONTOS, CONFRONTO_DIRETO, SALDO), jogos);
        assertThat(ids(porConfronto)).containsExactly(1L, 2L, 4L, 3L);
        assertThat(posicoes(porConfronto)).containsExactly(1, 2, 3, 4);
    }

    @Test
    void sorteioUsaAOrdemEmbaralhadaDaSeed() {
        Classificacao c = calcular(config(PONTOS, SORTEIO));

        // Random(42) embaralha [1, 2, 3, 4] em [4, 2, 1, 3]
        assertThat(ids(c)).containsExactly(4L, 2L, 1L, 3L);
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 4);
    }

    @Test
    void sorteioSoDesempataQuemContinuouEmpatado() {
        Classificacao c = calcular(config(PONTOS, SORTEIO), r(1, 2, 1, 0));

        assertThat(ids(c)).containsExactly(1L, 4L, 2L, 3L);
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 4);
    }

    @Test
    void sorteioEReprodutivelEDependeDaSeed() {
        ConfigPontosCorridos config = config(PONTOS, SORTEIO);
        List<ParticipanteMotor> oito = ParticipantesDeTeste.com(8);

        Classificacao primeira = formato.calcular(config, oito, List.of(), 7L);
        Classificacao segunda = formato.calcular(config, oito, List.of(), 7L);
        Set<List<Long>> ordens = new HashSet<>();
        for (long seed = 1; seed <= 10; seed++) {
            ordens.add(ids(formato.calcular(config, oito, List.of(), seed)));
        }

        assertThat(segunda).isEqualTo(primeira);
        assertThat(ordens.size()).isGreaterThan(1);
    }

    @Test
    void sorteioEAMesmaOrdemEmbaralhadaDaGeracaoDaTabela() {
        // Com n ímpar, a rodada k da tabela tem folga no k-ésimo da ordem sorteada
        for (long seed = 0; seed < 30; seed++) {
            List<ParticipanteMotor> cinco = ParticipantesDeTeste.com(5);
            ConfigPontosCorridos config = config(PONTOS, SORTEIO);

            List<Long> folgas = formato.gerar(config, cinco, seed).rodadas().stream().map(RodadaGerada::folgaId)
                    .toList();
            Classificacao c = formato.calcular(config, cinco, List.of(), seed);

            assertThat(ids(c)).as("seed %d", seed).isEqualTo(folgas);
        }
    }

    @Test
    void sorteioNaoDependeDaOrdemDeEntradaDosParticipantes() {
        List<ParticipanteMotor> invertidos = new ArrayList<>(quatro);
        Collections.reverse(invertidos);

        Classificacao c = formato.calcular(config(PONTOS, SORTEIO), invertidos, List.of(), SEED);

        assertThat(ids(c)).containsExactly(4L, 2L, 1L, 3L);
    }

    // ---------------------------------------------------------------- combinações e confronto direto

    @Test
    void ordemDosCriteriosMudaOResultado() {
        ResultadoJogo[] jogos = { r(2, 1, 1, 0), r(1, 3, 5, 0) };

        Classificacao confrontoAntes = calcular(config(PONTOS, CONFRONTO_DIRETO, SALDO), jogos);
        Classificacao saldoAntes = calcular(config(PONTOS, SALDO, CONFRONTO_DIRETO), jogos);

        assertThat(ids(confrontoAntes)).containsExactly(2L, 1L, 4L, 3L);
        assertThat(ids(saldoAntes)).containsExactly(1L, 2L, 4L, 3L);
        assertThat(posicoes(confrontoAntes)).containsExactly(1, 2, 3, 4);
        assertThat(posicoes(saldoAntes)).containsExactly(1, 2, 3, 4);
    }

    @Test
    void confrontoDiretoComTresEmpatadosQueSeparaParcialmente() {
        ResultadoJogo[] jogos = cicloComTresEmpatados();

        // 1, 2 e 3 têm 5 pontos. Mini-tabela entre os três: 3 faz 5, 1 e 2 fazem 4 cada
        Classificacao c = calcular(config(PONTOS, CONFRONTO_DIRETO, SALDO), jogos);
        assertThat(pontos(c)).containsExactly(5, 5, 5, 2);
        // 3 sobe pelo confronto direto; 1 e 2 seguem empatados e o saldo (2: +2, 1: 0) decide
        assertThat(ids(c)).containsExactly(3L, 2L, 1L, 4L);
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 4);

        // Sem outro critério, o confronto direto não é reaplicado só entre 1 e 2 (onde 1 venceu): dividem a posição
        Classificacao semSaldo = calcular(config(PONTOS, CONFRONTO_DIRETO), jogos);
        assertThat(ids(semSaldo)).containsExactly(3L, 1L, 2L, 4L);
        assertThat(posicoes(semSaldo)).containsExactly(1, 2, 2, 4);

        // Com o saldo antes, a ordem muda e o confronto direto vira o último recurso
        Classificacao saldoPrimeiro = calcular(config(PONTOS, SALDO, CONFRONTO_DIRETO), jogos);
        assertThat(ids(saldoPrimeiro)).containsExactly(2L, 1L, 3L, 4L);
    }

    @Test
    void confrontoDiretoIgnoraJogosContraQuemNaoEstaNoGrupoEmpatado() {
        // 1 e 2 empatam em 3 pontos. 1 perdeu para 2 no confronto direto, mas goleou 3: o saldo é só do geral
        Classificacao c = calcular(config(PONTOS, CONFRONTO_DIRETO), r(2, 1, 1, 0), r(1, 3, 9, 0));

        assertThat(ids(c)).containsExactly(2L, 1L, 3L, 4L);
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 3);
    }

    @Test
    void empateTotalDividePosicaoEAparecemEmOrdemDeId() {
        Classificacao c = calcular(config(PONTOS, VITORIAS, SALDO, PRO, CONFRONTO_DIRETO), r(4, 3, 1, 0), r(1, 2, 0, 0));

        assertThat(ids(c)).containsExactly(4L, 1L, 2L, 3L);
        assertThat(posicoes(c)).containsExactly(1, 2, 2, 4);
    }

    @Test
    void padraoComSorteioNuncaDeixaEmpateTotal() {
        Classificacao c = calcular(ConfigPontosCorridos.padrao(1));

        assertThat(ids(c)).containsExactly(4L, 2L, 1L, 3L);
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 4);
    }

    // ---------------------------------------------------------------- pontuação customizada

    @Test
    void pontuacaoCustomizadaDoisUmZero() {
        ConfigPontosCorridos config = new ConfigPontosCorridos(1, 2, 1, 0, true, List.of(PONTOS, VITORIAS));

        Classificacao c = formato.calcular(config, ParticipantesDeTeste.comIds(1, 2, 3),
                List.of(r(1, 2, 2, 1), r(2, 3, 1, 1), r(3, 1, 0, 1)), SEED);

        assertThat(ids(c)).containsExactly(1L, 2L, 3L);
        assertThat(pontos(c)).containsExactly(4, 1, 1);
        assertThat(posicoes(c)).containsExactly(1, 2, 2);
    }

    @Test
    void pontuacaoCustomizadaComDerrotaNegativaOrdenaPontosNegativos() {
        ConfigPontosCorridos config = new ConfigPontosCorridos(1, 2, 1, -1, true, List.of(PONTOS));

        Classificacao c = formato.calcular(config, ParticipantesDeTeste.comIds(1, 2, 3),
                List.of(r(1, 3, 0, 1), r(3, 1, 2, 0)), SEED);

        assertThat(ids(c)).containsExactly(3L, 2L, 1L);
        assertThat(pontos(c)).containsExactly(4, 0, -2);
        assertThat(posicoes(c)).containsExactly(1, 2, 3);
    }

    @Test
    void confrontoDiretoUsaAPontuacaoDaConfig() {
        // Com 2/1/-1, 1 (vitória e derrota contra 3) tem 2 - 1 = 1 ponto na mini-tabela entre 1, 2 e 3,
        // 2 (dois empates) tem 2 e 3 tem 3. Com 3/1/0 o 1 valeria 3 e passaria o 2: a config é que manda.
        ConfigPontosCorridos config = new ConfigPontosCorridos(1, 2, 1, -1, true, List.of(PONTOS, CONFRONTO_DIRETO));
        List<ResultadoJogo> jogos = List.of(r(1, 3, 1, 0), r(3, 1, 1, 0), r(2, 3, 0, 0), r(3, 2, 0, 0),
                r(1, 4, 1, 0), r(2, 4, 0, 0));

        Classificacao c = formato.calcular(config, quatro, jogos, SEED);

        assertThat(pontos(c)).containsExactly(3, 3, 3, 0);
        assertThat(ids(c)).containsExactly(3L, 2L, 1L, 4L);
        assertThat(posicoes(c)).containsExactly(1, 2, 3, 4);
    }

    // ---------------------------------------------------------------- invariância e validação

    @Test
    void ordemDeEntradaDeParticipantesEResultadosNaoMudaAClassificacao() {
        ConfigPontosCorridos config = ConfigPontosCorridos.padrao(2);
        List<ResultadoJogo> jogos = Arrays.asList(cicloComTresEmpatados());
        Classificacao esperada = formato.calcular(config, quatro, jogos, SEED);

        List<ParticipanteMotor> participantes = new ArrayList<>(quatro);
        List<ResultadoJogo> embaralhados = new ArrayList<>(jogos);
        for (long seed = 0; seed < 10; seed++) {
            Collections.shuffle(participantes, new Random(seed));
            Collections.shuffle(embaralhados, new Random(seed + 100));

            assertThat(formato.calcular(config, participantes, embaralhados, SEED)).isEqualTo(esperada);
        }
    }

    @Test
    void posicoesDeCompeticaoComMuitosParticipantes() {
        List<ParticipanteMotor> vinte = ParticipantesDeTeste.com(20);
        long[] ids = vinte.stream().mapToLong(ParticipanteMotor::id).toArray();
        List<ResultadoJogo> empates = new ArrayList<>();
        for (int i = 0; i < ids.length - 1; i++) {
            empates.add(r(ids[i], ids[i + 1], 1, 1));
        }

        Classificacao c = formato.calcular(config(PONTOS), vinte, empates, SEED);

        // os 18 do meio fizeram 2 empates (2 pontos); as pontas fizeram 1 (1 ponto)
        assertThat(c.linhas()).hasSize(20);
        assertThat(posicoes(c)).containsExactlyElementsOf(
                Stream.concat(Collections.nCopies(18, 1).stream(), Stream.of(19, 19)).toList());
        assertThat(ids(c).subList(18, 20)).containsExactly(ids[0], ids[19]);
    }

    @Test
    void recusaParticipanteDesconhecidoNoResultado() {
        assertThatThrownBy(() -> calcular(config(PONTOS), r(1, 9, 1, 0)))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("desconhecido")
                .hasMessageContaining("9");
        assertThatThrownBy(() -> calcular(config(PONTOS), r(9, 1, 1, 0)))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("9");
    }

    @Test
    void recusaEmpateNosResultadosQuandoNaoPermitido() {
        ConfigPontosCorridos semEmpate = new ConfigPontosCorridos(1, 3, 1, 0, false, List.of(PONTOS));

        assertThatThrownBy(() -> formato.calcular(semEmpate, quatro, List.of(r(1, 2, 1, 0), r(3, 4, 2, 2)), SEED))
                .isInstanceOf(ResultadoInvalidoException.class)
                .hasMessageContaining("não permite empate");
    }

    @Test
    void recusaResultadosNulosOuComItemNulo() {
        assertThatThrownBy(() -> formato.calcular(config(PONTOS), quatro, null, SEED))
                .isInstanceOf(ResultadoInvalidoException.class);
        assertThatThrownBy(() -> formato.calcular(config(PONTOS), quatro, Arrays.asList(r(1, 2, 1, 0), null), SEED))
                .isInstanceOf(ResultadoInvalidoException.class);
    }

    @Test
    void calcularRecusaParticipantesEConfigInvalidos() {
        assertThatThrownBy(() -> formato.calcular(config(PONTOS), ParticipantesDeTeste.com(1), List.of(), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class);
        assertThatThrownBy(() -> formato.calcular(config(PONTOS), ParticipantesDeTeste.comIds(1, 1), List.of(), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class);
        assertThatThrownBy(() -> formato.calcular(null, quatro, List.of(), SEED))
                .isInstanceOf(ConfigInvalidaException.class);
    }

    @Test
    void classificacaoCopiaAsLinhasEEImutavel() {
        List<LinhaClassificacao> original = new ArrayList<>(calcular(config(PONTOS)).linhas());
        Classificacao c = new Classificacao(original);

        original.clear();

        assertThat(c.linhas()).hasSize(4);
        assertThatThrownBy(() -> c.linhas().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void tabelaERodadaCopiamAsListasEHaImutabilidade() {
        List<RodadaGerada> rodadas = new ArrayList<>(
                formato.gerar(ConfigPontosCorridos.padrao(1), quatro, SEED).rodadas());
        TabelaPontosCorridos tabela = new TabelaPontosCorridos(rodadas);
        List<JogoGerado> jogos = new ArrayList<>(List.of(new JogoGerado(1, 2)));
        RodadaGerada rodada = new RodadaGerada(1, 1, jogos, null);

        rodadas.clear();
        jogos.clear();

        assertThat(tabela.rodadas()).hasSize(3);
        assertThat(rodada.jogos()).hasSize(1);
        assertThatThrownBy(() -> tabela.rodadas().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rodada.jogos().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
