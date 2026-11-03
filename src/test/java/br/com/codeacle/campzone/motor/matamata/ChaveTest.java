package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.FORMATO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.SEED;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comSeeding;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.confronto;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.fase;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idDaSeed;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogoUnico;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.participantes;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesDeTeste;

/** Estrutura da chave gerada: tamanho, byes, fases, ordem clássica e determinismo. */
class ChaveTest {

    private final ConfigMataMata config = jogoUnico(PENALTIS);

    private EstadoMataMata gerar(int n) {
        return FORMATO.gerar(config, participantes(n), SEED);
    }

    /** n, P, byes e os nomes das fases, em ordem. */
    static Stream<Arguments> chaves() {
        return Stream.of(
                Arguments.of(2, 2, 0, "Final"),
                Arguments.of(3, 4, 1, "Semifinal|Final"),
                Arguments.of(4, 4, 0, "Semifinal|Final"),
                Arguments.of(5, 8, 3, "Quartas de final|Semifinal|Final"),
                Arguments.of(6, 8, 2, "Quartas de final|Semifinal|Final"),
                Arguments.of(7, 8, 1, "Quartas de final|Semifinal|Final"),
                Arguments.of(8, 8, 0, "Quartas de final|Semifinal|Final"),
                Arguments.of(9, 16, 7, "Oitavas de final|Quartas de final|Semifinal|Final"),
                Arguments.of(11, 16, 5, "Oitavas de final|Quartas de final|Semifinal|Final"),
                Arguments.of(16, 16, 0, "Oitavas de final|Quartas de final|Semifinal|Final"),
                Arguments.of(17, 32, 15, "Fase de 32|Oitavas de final|Quartas de final|Semifinal|Final"),
                Arguments.of(33, 64, 31, "Fase de 64|Fase de 32|Oitavas de final|Quartas de final|Semifinal|Final"));
    }

    static Stream<Integer> tamanhosObrigatorios() {
        return Stream.of(2, 3, 4, 5, 8, 11, 16);
    }

    /** Chaves com pelo menos duas metades de 2 confrontos cada (com 2 e 3 participantes só há uma semifinal ou final). */
    static Stream<Integer> tamanhosComMetades() {
        return Stream.of(5, 8, 11, 16);
    }

    // ---------------------------------------------------------------- tamanho, byes e fases

    @ParameterizedTest(name = "n={0} -> chave {1}, {2} byes")
    @MethodSource("chaves")
    void tamanhoDaChaveByesEFases(int n, int tamanho, int byes, String nomes) {
        EstadoMataMata estado = gerar(n);

        List<String> esperados = List.of(nomes.split("\\|"));
        assertThat(estado.fases()).extracting(FaseEstado::nome).containsExactlyElementsOf(esperados);
        assertThat(estado.fases()).extracting(FaseEstado::ordem)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, esperados.size()).boxed().toList());
        // quantidade de confrontos: P/2 na primeira fase, e metade a cada fase seguinte
        for (int i = 0; i < estado.fases().size(); i++) {
            assertThat(estado.fases().get(i).confrontos()).hasSize(tamanho / 2 >> i);
        }
        assertThat(fase(estado, 1).confrontos().stream().filter(ConfrontoEstado::bye)).hasSize(byes);
        assertThat(tamanho - n).isEqualTo(byes);
    }

    @ParameterizedTest(name = "n={0}")
    @MethodSource("tamanhosObrigatorios")
    void soAUltimaFaseEFinalEAsOutrasSaoEliminatorias(int n) {
        EstadoMataMata estado = gerar(n);

        List<FaseEstado> fases = estado.fases();
        assertThat(fases.subList(0, fases.size() - 1)).allSatisfy(f -> assertThat(f.tipo()).isEqualTo(TipoFase.ELIMINATORIA));
        assertThat(fases.get(fases.size() - 1).tipo()).isEqualTo(TipoFase.FINAL);
        assertThat(fases.get(fases.size() - 1).confrontos()).hasSize(1);
    }

    @ParameterizedTest(name = "n={0}")
    @MethodSource("tamanhosObrigatorios")
    void geradaNaoTemCampeaoViceNemTerceiro(int n) {
        EstadoMataMata estado = gerar(n);

        // só o chaveamento sem jogos disputados; os byes já avançam, mas ninguém é campeão antes da final
        assertThat(estado.campeaoId()).isNull();
        assertThat(estado.viceId()).isNull();
        assertThat(estado.terceiroId()).isNull();
    }

    // ---------------------------------------------------------------- byes e seeds

    @ParameterizedTest(name = "n={0}")
    @MethodSource("tamanhosObrigatorios")
    void byesVaoParaAsMelhoresSeedsEJaNascemComVencedor(int n) {
        EstadoMataMata estado = gerar(n);
        int tamanho = Integer.highestOneBit(n - 1) * 2;
        int byes = tamanho - n;

        List<ConfrontoEstado> comBye = fase(estado, 1).confrontos().stream().filter(ConfrontoEstado::bye).toList();
        assertThat(comBye).extracting(ConfrontoEstado::participanteAId)
                .containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, byes).mapToObj(s -> idDaSeed(s)).toList());
        assertThat(comBye).allSatisfy(c -> {
            assertThat(c.participanteBId()).isNull();
            assertThat(c.vencedorId()).isEqualTo(c.participanteAId());
            assertThat(c.jogos()).isEmpty();
        });
    }

    @ParameterizedTest(name = "n={0}")
    @MethodSource("tamanhosObrigatorios")
    void confrontosSemByeNascemSemVencedorEComJogoPendente(int n) {
        EstadoMataMata estado = gerar(n);

        List<ConfrontoEstado> reais = fase(estado, 1).confrontos().stream().filter(c -> !c.bye()).toList();
        assertThat(reais).allSatisfy(c -> {
            assertThat(c.participanteAId()).isNotNull();
            assertThat(c.participanteBId()).isNotNull();
            assertThat(c.vencedorId()).isNull();
            assertThat(c.jogos()).extracting(JogoEstado::situacao).containsOnly(SituacaoJogo.PENDENTE);
        });
    }

    @Test
    void ordemClassicaDaPrimeiraFaseComOitoParticipantes() {
        EstadoMataMata estado = gerar(8);

        assertThat(fase(estado, 1).confrontos()).extracting(c -> par(c)).containsExactly(
                par(1, 8), par(4, 5), par(2, 7), par(3, 6));
    }

    @Test
    void ordemClassicaDaPrimeiraFaseComOnzeParticipantes() {
        EstadoMataMata estado = gerar(11);

        // P=16: 1x16(bye) 8x9 4x13(bye) 5x12(bye) 2x15(bye) 7x10 3x14(bye) 6x11
        assertThat(fase(estado, 1).confrontos()).extracting(c -> par(c)).containsExactly(
                par(1, 0), par(8, 9), par(4, 0), par(5, 0), par(2, 0), par(7, 10), par(3, 0), par(6, 11));
    }

    @ParameterizedTest(name = "n={0}")
    @MethodSource("tamanhosComMetades")
    void umEDoisFicamEmMetadesOpostasEOAEhSempreAMelhorSeed(int n) {
        EstadoMataMata estado = gerar(n);
        List<ConfrontoEstado> primeiraFase = fase(estado, 1).confrontos();
        int metade = primeiraFase.size() / 2;

        int posicaoDoUm = posicaoDaSeed(primeiraFase, 1);
        int posicaoDoDois = posicaoDaSeed(primeiraFase, 2);
        assertThat(posicaoDoUm).isLessThanOrEqualTo(metade);
        assertThat(posicaoDoDois).isGreaterThan(metade);
        assertThat(primeiraFase).allSatisfy(c -> {
            if (!c.bye()) {
                assertThat(seedDe(c.participanteAId())).isLessThan(seedDe(c.participanteBId()));
            }
        });
    }

    // ---------------------------------------------------------------- determinismo

    @ParameterizedTest(name = "n={0}")
    @MethodSource("tamanhosObrigatorios")
    void mesmaSeedDaMesmaChaveEAOrdemDeEntradaNaoImporta(int n) {
        ConfigMataMata aleatorio = comSeeding(config, Seeding.ALEATORIO);
        List<ParticipanteMotor> ordenados = ParticipantesDeTeste.com(n);
        List<ParticipanteMotor> embaralhados = new ArrayList<>(ordenados);
        Collections.shuffle(embaralhados, new Random(77));
        List<ParticipanteMotor> invertidos = new ArrayList<>(ordenados);
        Collections.reverse(invertidos);

        EstadoMataMata esperado = FORMATO.gerar(aleatorio, ordenados, SEED);

        assertThat(FORMATO.gerar(aleatorio, ordenados, SEED)).isEqualTo(esperado);
        assertThat(FORMATO.gerar(aleatorio, embaralhados, SEED)).isEqualTo(esperado);
        assertThat(FORMATO.gerar(aleatorio, invertidos, SEED)).isEqualTo(esperado);
    }

    @Test
    void seedsDeSorteioDiferentesPodemDarChavesDiferentes() {
        ConfigMataMata aleatorio = comSeeding(config, Seeding.ALEATORIO);
        Set<EstadoMataMata> chaves = new HashSet<>();
        for (long seed = 1; seed <= 10; seed++) {
            chaves.add(FORMATO.gerar(aleatorio, ParticipantesDeTeste.com(8), seed));
        }

        assertThat(chaves.size()).isGreaterThan(1);
    }

    @Test
    void nomesDosParticipantesNaoInfluenciamAChave() {
        List<ParticipanteMotor> renomeados = participantes(8).stream()
                .map(p -> new ParticipanteMotor(p.id(), "Outro " + p.id(), p.seed())).toList();

        assertThat(FORMATO.gerar(config, renomeados, SEED)).isEqualTo(gerar(8));
    }

    // ---------------------------------------------------------------- auxiliares

    /** Seeds (A, B) do confronto; B vale 0 no bye. */
    private static List<Integer> par(ConfrontoEstado c) {
        return List.of(seedDe(c.participanteAId()), c.participanteBId() == null ? 0 : seedDe(c.participanteBId()));
    }

    private static List<Integer> par(int a, int b) {
        return List.of(a, b);
    }

    private static int seedDe(long id) {
        return (int) ((id - 10) / 7) + 1;
    }

    /** Posição (a partir de 1) do confronto da primeira fase em que a seed joga. */
    private static int posicaoDaSeed(List<ConfrontoEstado> confrontos, int seed) {
        return confronto(confrontos, seed).posicao();
    }

    private static ConfrontoEstado confronto(List<ConfrontoEstado> confrontos, int seed) {
        return confrontos.stream().filter(c -> c.participanteAId() == idDaSeed(seed)
                || (c.participanteBId() != null && c.participanteBId() == idDaSeed(seed))).findFirst().orElseThrow();
    }
}
