package br.com.codeacle.campzone.motor.pontoscorridos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesDeTeste;

/** Geração da tabela round-robin: estrutura, mando, quebras, determinismo e turnos. */
class GeracaoTabelaTest {

    private static final long SEED = 20260;

    private final PontosCorridosFormato formato = new PontosCorridosFormato();

    private TabelaPontosCorridos gerar(int n, int turnos, long seed) {
        return formato.gerar(ConfigPontosCorridos.padrao(turnos), ParticipantesDeTeste.com(n), seed);
    }

    /** Os tamanhos obrigatórios da spec e mais alguns, com 1, 2 e 3 turnos e seeds variadas. */
    static Stream<Arguments> cenarios() {
        return IntStream.rangeClosed(2, 17).boxed().flatMap(n -> Stream.of(1, 2, 3)
                .flatMap(turnos -> Stream.of(0L, 7L, -42L).map(seed -> Arguments.of(n, turnos, seed))));
    }

    static Stream<Arguments> tamanhosObrigatorios() {
        return Stream.of(2, 3, 4, 5, 8, 9).flatMap(n -> Stream.of(1, 2, 3).map(turnos -> Arguments.of(n, turnos)));
    }

    // ---------------------------------------------------------------- estrutura

    @ParameterizedTest(name = "n={0}, turnos={1}")
    @MethodSource("tamanhosObrigatorios")
    void quantidadeDeRodadasEJogos(int n, int turnos) {
        TabelaPontosCorridos tabela = gerar(n, turnos, SEED);

        int rodadasPorTurno = n % 2 == 0 ? n - 1 : n;
        assertThat(tabela.rodadas()).hasSize(rodadasPorTurno * turnos);
        assertThat(tabela.rodadas()).allSatisfy(r -> assertThat(r.jogos()).hasSize(n / 2));
        long totalDeJogos = tabela.rodadas().stream().mapToLong(r -> r.jogos().size()).sum();
        assertThat(totalDeJogos).isEqualTo((long) n * (n - 1) / 2 * turnos);
    }

    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenarios")
    void todosEnfrentamTodosExatamenteUmaVezPorTurno(int n, int turnos, long seed) {
        TabelaPontosCorridos tabela = gerar(n, turnos, seed);
        Set<Long> ids = ParticipantesDeTeste.com(n).stream().map(ParticipanteMotor::id).collect(Collectors.toSet());

        for (int turno = 1; turno <= turnos; turno++) {
            Set<Set<Long>> confrontos = new HashSet<>();
            int jogos = 0;
            for (RodadaGerada rodada : rodadasDoTurno(tabela, turno)) {
                for (JogoGerado jogo : rodada.jogos()) {
                    jogos++;
                    assertThat(jogo.mandanteId()).isNotEqualTo(jogo.visitanteId());
                    assertThat(ids).contains(jogo.mandanteId(), jogo.visitanteId());
                    confrontos.add(Set.of(jogo.mandanteId(), jogo.visitanteId()));
                }
            }
            assertThat(jogos).as("jogos no turno %d", turno).isEqualTo(n * (n - 1) / 2);
            assertThat(confrontos).as("confrontos distintos no turno %d", turno).hasSize(n * (n - 1) / 2);
        }
    }

    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenarios")
    void ninguemJogaDuasVezesNaMesmaRodada(int n, int turnos, long seed) {
        Set<Long> ids = ParticipantesDeTeste.com(n).stream().map(ParticipanteMotor::id).collect(Collectors.toSet());

        for (RodadaGerada rodada : gerar(n, turnos, seed).rodadas()) {
            List<Long> presentes = new ArrayList<>();
            rodada.jogos().forEach(j -> {
                presentes.add(j.mandanteId());
                presentes.add(j.visitanteId());
            });
            if (rodada.folgaId() != null) {
                presentes.add(rodada.folgaId());
            }
            assertThat(presentes).as("rodada %d", rodada.numero()).doesNotHaveDuplicates()
                    .containsExactlyInAnyOrderElementsOf(ids);
        }
    }

    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenarios")
    void folgaSoComNumeroImparEUmaVezPorParticipanteEPorTurno(int n, int turnos, long seed) {
        TabelaPontosCorridos tabela = gerar(n, turnos, seed);

        if (n % 2 == 0) {
            assertThat(tabela.rodadas()).allSatisfy(r -> assertThat(r.folgaId()).isNull());
            return;
        }
        Set<Long> ids = ParticipantesDeTeste.com(n).stream().map(ParticipanteMotor::id).collect(Collectors.toSet());
        for (int turno = 1; turno <= turnos; turno++) {
            List<Long> folgas = rodadasDoTurno(tabela, turno).stream().map(RodadaGerada::folgaId).toList();
            assertThat(folgas).as("uma folga por rodada no turno %d", turno).doesNotContainNull()
                    .containsExactlyInAnyOrderElementsOf(ids);
        }
    }

    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenarios")
    void numeracaoContinuaEntreOsTurnos(int n, int turnos, long seed) {
        TabelaPontosCorridos tabela = gerar(n, turnos, seed);
        int porTurno = n % 2 == 0 ? n - 1 : n;

        for (int i = 0; i < tabela.rodadas().size(); i++) {
            assertThat(tabela.rodadas().get(i).numero()).isEqualTo(i + 1);
            assertThat(tabela.rodadas().get(i).turno()).isEqualTo(i / porTurno + 1);
        }
    }

    @Test
    void doisParticipantesTemUmaRodadaPorTurno() {
        TabelaPontosCorridos tabela = gerar(2, 2, SEED);

        assertThat(tabela.rodadas()).hasSize(2);
        JogoGerado ida = tabela.rodadas().get(0).jogos().get(0);
        JogoGerado volta = tabela.rodadas().get(1).jogos().get(0);
        assertThat(volta.mandanteId()).isEqualTo(ida.visitanteId());
        assertThat(volta.visitanteId()).isEqualTo(ida.mandanteId());
    }

    // ---------------------------------------------------------------- mando

    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenarios")
    void mandoEquilibradoEmCadaTurno(int n, int turnos, long seed) {
        TabelaPontosCorridos tabela = gerar(n, turnos, seed);

        for (int turno = 1; turno <= turnos; turno++) {
            Map<Long, Integer> saldoDeMando = new HashMap<>();
            for (RodadaGerada rodada : rodadasDoTurno(tabela, turno)) {
                for (JogoGerado jogo : rodada.jogos()) {
                    saldoDeMando.merge(jogo.mandanteId(), 1, Integer::sum);
                    saldoDeMando.merge(jogo.visitanteId(), -1, Integer::sum);
                }
            }
            assertThat(saldoDeMando).as("turno %d", turno).hasSize(n);
            // n par: n-1 jogos (ímpar), diferença de 1; n ímpar: n-1 jogos (par), diferença 0
            int maximo = n % 2 == 0 ? 1 : 0;
            assertThat(saldoDeMando.values()).allSatisfy(s -> assertThat(Math.abs(s)).isLessThanOrEqualTo(maximo));
        }
    }

    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenarios")
    void quebrasDeMandoDentroDoTurnoFicamNoMinimoTeorico(int n, int turnos, long seed) {
        TabelaPontosCorridos tabela = gerar(n, turnos, seed);

        for (int turno = 1; turno <= turnos; turno++) {
            int quebras = quebrasNoTurno(tabela, turno, false);
            if (n % 2 == 0) {
                // n par: o mínimo teórico é n-2 quebras por turno (e a construção o atinge)
                assertThat(quebras).as("n=%d turno %d", n, turno).isEqualTo(n - 2);
            } else {
                // n ímpar: a folga cai onde a quebra estaria; o total medido é 0, de qualquer forma que se conte
                assertThat(quebras).as("n=%d turno %d", n, turno).isZero();
                assertThat(quebrasNoTurno(tabela, turno, true)).as("pulando a folga, n=%d turno %d", n, turno).isZero();
            }
        }
    }

    @Test
    void cadaParticipanteTemNoMaximoUmaQuebraNoTurnoComNumeroPar() {
        TabelaPontosCorridos tabela = gerar(8, 1, SEED);

        Map<Long, Integer> porParticipante = quebrasPorParticipante(tabela, 1, false);

        assertThat(porParticipante.values()).allSatisfy(q -> assertThat(q).isLessThanOrEqualTo(1));
        assertThat(porParticipante.values().stream().filter(q -> q == 0)).hasSize(2);
    }

    // ---------------------------------------------------------------- turnos

    static Stream<Arguments> cenariosComMaisDeUmTurno() {
        return cenarios().filter(argumentos -> (int) argumentos.get()[1] >= 2);
    }

    /**
     * Trava o comportamento atual na virada entre turnos: o turno seguinte repete o anterior (com o mando invertido
     * nos pares), então quase todos repetem o mando entre a última rodada de um turno e a primeira do próximo. Só
     * dois escapam: com {@code n} par, o fixo e o último do círculo; com {@code n} ímpar, os dois que folgam numa
     * das rodadas. O total é {@code n-2} em qualquer virada. Se a geração melhorar, este teste deve mudar de
     * propósito.
     */
    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenariosComMaisDeUmTurno")
    void viradaEntreTurnosTemNMenosDoisQuebrasDeMando(int n, int turnos, long seed) {
        TabelaPontosCorridos tabela = gerar(n, turnos, seed);

        for (int turno = 1; turno < turnos; turno++) {
            RodadaGerada ultima = rodadasDoTurno(tabela, turno).getLast();
            RodadaGerada primeira = rodadasDoTurno(tabela, turno + 1).getFirst();

            assertThat(quebrasNaVirada(ultima, primeira)).as("virada do turno %d para o %d, n=%d", turno, turno + 1, n)
                    .isEqualTo(n - 2);
        }
    }

    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenarios")
    void turnosSeRepetemComMandoInvertidoNosPares(int n, int turnos, long seed) {
        TabelaPontosCorridos tabela = gerar(n, turnos, seed);
        List<RodadaGerada> primeiro = rodadasDoTurno(tabela, 1);

        for (int turno = 2; turno <= turnos; turno++) {
            List<RodadaGerada> atual = rodadasDoTurno(tabela, turno);
            assertThat(atual).hasSameSizeAs(primeiro);
            for (int i = 0; i < primeiro.size(); i++) {
                List<JogoGerado> esperado = primeiro.get(i).jogos();
                if (turno % 2 == 0) {
                    esperado = esperado.stream().map(j -> new JogoGerado(j.visitanteId(), j.mandanteId())).toList();
                }
                assertThat(atual.get(i).jogos()).as("turno %d, rodada %d", turno, i + 1).isEqualTo(esperado);
                assertThat(atual.get(i).folgaId()).isEqualTo(primeiro.get(i).folgaId());
            }
        }
    }

    @Test
    void idaEVoltaFazCadaParEnfrentarSeUmaVezEmCadaMando() {
        TabelaPontosCorridos tabela = gerar(6, 2, SEED);

        Set<JogoGerado> jogos = tabela.rodadas().stream().flatMap(r -> r.jogos().stream())
                .collect(Collectors.toSet());

        assertThat(jogos).hasSize(6 * 5);
        for (JogoGerado jogo : jogos) {
            assertThat(jogos).contains(new JogoGerado(jogo.visitanteId(), jogo.mandanteId()));
        }
    }

    @Test
    void tresTurnosRepetemOPrimeiroNoTerceiro() {
        TabelaPontosCorridos tabela = gerar(5, 3, SEED);

        assertThat(tabela.rodadas()).hasSize(15);
        for (int i = 0; i < 5; i++) {
            assertThat(tabela.rodadas().get(10 + i).jogos()).isEqualTo(tabela.rodadas().get(i).jogos());
            assertThat(tabela.rodadas().get(5 + i).jogos()).isNotEqualTo(tabela.rodadas().get(i).jogos());
        }
    }

    // ---------------------------------------------------------------- determinismo

    @ParameterizedTest(name = "n={0}, turnos={1}, seed={2}")
    @MethodSource("cenarios")
    void mesmaSeedDaMesmaTabelaEAOrdemDeEntradaNaoImporta(int n, int turnos, long seed) {
        TabelaPontosCorridos esperada = gerar(n, turnos, seed);
        List<ParticipanteMotor> embaralhados = new ArrayList<>(ParticipantesDeTeste.com(n));
        Collections.shuffle(embaralhados, new Random(99));
        List<ParticipanteMotor> invertidos = new ArrayList<>(ParticipantesDeTeste.com(n));
        Collections.reverse(invertidos);
        ConfigPontosCorridos config = ConfigPontosCorridos.padrao(turnos);

        assertThat(gerar(n, turnos, seed)).isEqualTo(esperada);
        assertThat(formato.gerar(config, embaralhados, seed)).isEqualTo(esperada);
        assertThat(formato.gerar(config, invertidos, seed)).isEqualTo(esperada);
    }

    @Test
    void seedsDiferentesPodemDarTabelasDiferentes() {
        Set<TabelaPontosCorridos> tabelas = new HashSet<>();
        for (long seed = 1; seed <= 10; seed++) {
            tabelas.add(gerar(8, 1, seed));
        }

        assertThat(tabelas.size()).isGreaterThan(1);
    }

    @Test
    void nomesESeedsDosParticipantesNaoInfluenciamATabela() {
        List<ParticipanteMotor> renomeados = ParticipantesDeTeste.com(6).stream()
                .map(p -> new ParticipanteMotor(p.id(), "Outro nome " + p.id(), 1)).toList();

        assertThat(formato.gerar(ConfigPontosCorridos.padrao(2), renomeados, SEED)).isEqualTo(gerar(6, 2, SEED));
    }

    /**
     * Trava a saída byte a byte: o algoritmo de {@code Random(seed)} e de {@code Collections.shuffle} é fixado em
     * contrato pelo JDK, então esta tabela não pode mudar entre versões nem execuções.
     */
    @Test
    void tabelaDeQuatroParticipantesIdaEVoltaComSeed42() {
        TabelaPontosCorridos tabela = formato.gerar(ConfigPontosCorridos.padrao(2),
                ParticipantesDeTeste.comIds(1, 2, 3, 4), 42L);

        // Com a seed 42, a ordem sorteada dos ids 1..4 é [4, 2, 1, 3]
        assertThat(descrever(tabela)).containsExactly(
                "R1 T1 3x4 1x2",
                "R2 T1 2x3 4x1",
                "R3 T1 3x1 2x4",
                "R4 T2 4x3 2x1",
                "R5 T2 3x2 1x4",
                "R6 T2 1x3 4x2");
    }

    // ---------------------------------------------------------------- auxiliares

    private static List<String> descrever(TabelaPontosCorridos tabela) {
        return tabela.rodadas().stream().map(r -> "R" + r.numero() + " T" + r.turno() + " "
                + r.jogos().stream().map(j -> j.mandanteId() + "x" + j.visitanteId()).collect(Collectors.joining(" ")))
                .toList();
    }

    private static List<RodadaGerada> rodadasDoTurno(TabelaPontosCorridos tabela, int turno) {
        return tabela.rodadas().stream().filter(r -> r.turno() == turno).toList();
    }

    /** Participantes que jogam nas duas rodadas e têm o mesmo mando nelas (quem folga numa delas não conta). */
    private static int quebrasNaVirada(RodadaGerada ultima, RodadaGerada primeira) {
        Map<Long, Boolean> mandoNaUltima = mandos(ultima);
        Map<Long, Boolean> mandoNaPrimeira = mandos(primeira);
        return (int) mandoNaUltima.entrySet().stream()
                .filter(e -> e.getValue().equals(mandoNaPrimeira.get(e.getKey()))).count();
    }

    /** Mando de cada participante que joga na rodada: {@code true} em casa, {@code false} fora. */
    private static Map<Long, Boolean> mandos(RodadaGerada rodada) {
        Map<Long, Boolean> mandos = new HashMap<>();
        rodada.jogos().forEach(j -> {
            mandos.put(j.mandanteId(), true);
            mandos.put(j.visitanteId(), false);
        });
        return mandos;
    }

    private static int quebrasNoTurno(TabelaPontosCorridos tabela, int turno, boolean pularFolga) {
        return quebrasPorParticipante(tabela, turno, pularFolga).values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Quebras de mando por participante dentro de um turno: jogar em casa (ou fora) duas vezes seguidas. Sem
     * {@code pularFolga}, a rodada de folga interrompe a sequência; com ele, compara-se o jogo anterior com o
     * seguinte, ignorando a rodada de folga entre eles.
     */
    private static Map<Long, Integer> quebrasPorParticipante(TabelaPontosCorridos tabela, int turno,
            boolean pularFolga) {
        Map<Long, List<Boolean>> mandos = new HashMap<>();
        for (RodadaGerada rodada : rodadasDoTurno(tabela, turno)) {
            Map<Long, Boolean> daRodada = new HashMap<>();
            rodada.jogos().forEach(j -> {
                daRodada.put(j.mandanteId(), true);
                daRodada.put(j.visitanteId(), false);
            });
            if (rodada.folgaId() != null) {
                daRodada.put(rodada.folgaId(), null);
            }
            daRodada.forEach((id, mando) -> mandos.computeIfAbsent(id, k -> new ArrayList<>()).add(mando));
        }
        Map<Long, Integer> quebras = new HashMap<>();
        mandos.forEach((id, sequencia) -> {
            int total = 0;
            Boolean anterior = null;
            for (Boolean mando : sequencia) {
                if (mando == null && !pularFolga) {
                    anterior = null;
                } else if (mando != null) {
                    if (anterior != null && anterior.equals(mando)) {
                        total++;
                    }
                    anterior = mando;
                }
            }
            quebras.put(id, total);
        });
        return quebras;
    }
}
