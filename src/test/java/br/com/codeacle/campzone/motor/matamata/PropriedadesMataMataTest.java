package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.SORTEIO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.FORMATO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.SEED;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comSeeding;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comTerceiro;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idaEVolta;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogarAteOFim;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogoUnico;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.ocupantes;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.participantes;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.placar;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.serie;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.vence;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesDeTeste;
import br.com.codeacle.campzone.motor.matamata.MataMataFixtures.Torneio;

/** Propriedades que valem para qualquer tamanho de chave, tipo de confronto, seeding e rumo do torneio. */
class PropriedadesMataMataTest {

    /** Todos os tipos de confronto, com e sem 3º lugar. */
    static Stream<ConfigMataMata> configs() {
        return Stream.of(jogoUnico(PENALTIS), idaEVolta(PENALTIS), serie(3, PENALTIS), serie(5, SORTEIO))
                .flatMap(base -> Stream.of(comTerceiro(base, false), comTerceiro(base, true)));
    }

    /** n de 2 a 17, para cada config: cobre chaves de 2, 4, 8, 16 e 32 posições, com e sem byes. */
    static Stream<Arguments> tamanhosEConfigs() {
        return IntStream.rangeClosed(2, 17).boxed().flatMap(n -> configs().map(config -> Arguments.of(n, config)));
    }

    /**
     * Cada confronto tem um vencedor aleatório (mas determinístico, pela seed do teste), que vence todos os seus jogos:
     * assim o agregado da ida e volta e a maioria da série nunca empatam.
     */
    private static MataMataFixtures.Politica aleatoria(long seed) {
        return (fase, confronto, jogo, seeds) -> {
            boolean aVence = new Random(seed * 1_000_003L + fase.ordem() * 1_009L + confronto.posicao()).nextBoolean();
            long vencedor = aVence ? confronto.participanteAId() : confronto.participanteBId();
            return vence(fase, confronto, jogo, jogo.mandanteId() == vencedor);
        };
    }

    // ---------------------------------------------------------------- gerar == calcular sem resultados

    @ParameterizedTest(name = "n={0}, {1}")
    @MethodSource("tamanhosEConfigs")
    void gerarEquivaleACalcularSemResultados(int n, ConfigMataMata config) {
        for (Seeding seeding : Seeding.values()) {
            ConfigMataMata comSeeding = comSeeding(config, seeding);
            List<ParticipanteMotor> participantes = seeding == Seeding.ALEATORIO ? ParticipantesDeTeste.com(n)
                    : participantes(n);

            assertThat(FORMATO.gerar(comSeeding, participantes, SEED))
                    .isEqualTo(FORMATO.calcular(comSeeding, participantes, new ArrayList<>(), SEED));
            assertThat(FORMATO.gerar(comSeeding, participantes, SEED))
                    .isEqualTo(FORMATO.calcular(comSeeding, participantes, List.of(), SEED));
        }
    }

    // ---------------------------------------------------------------- a ordem dos resultados não importa

    @ParameterizedTest(name = "n={0}, {1}")
    @MethodSource("tamanhosEConfigs")
    void aOrdemDaListaDeResultadosNaoMudaOEstado(int n, ConfigMataMata config) {
        List<ParticipanteMotor> participantes = participantes(n);
        Torneio torneio = jogarAteOFim(config, participantes, SEED, aleatoria(n));
        List<ResultadoMataMata> resultados = torneio.resultados();
        Random embaralhador = new Random(n);

        // com todos os resultados e com prefixos (torneio em andamento)
        for (int quantos : new int[] { resultados.size(), resultados.size() / 2, 1 }) {
            List<ResultadoMataMata> prefixo = resultados.subList(0, Math.min(quantos, resultados.size()));
            EstadoMataMata esperado = FORMATO.calcular(config, participantes, prefixo, SEED);
            for (int tentativa = 0; tentativa < 5; tentativa++) {
                List<ResultadoMataMata> embaralhados = new ArrayList<>(prefixo);
                Collections.shuffle(embaralhados, embaralhador);

                assertThat(FORMATO.calcular(config, participantes, embaralhados, SEED)).isEqualTo(esperado);
            }
        }
    }

    @ParameterizedTest(name = "n={0}")
    @MethodSource("tamanhos")
    void aOrdemDaListaNaoMudaOEstadoQuandoOSorteioDecideTudo(int n) {
        // todo jogo termina 0 x 0 e o confronto é decidido pelo sorteio de cada confronto
        ConfigMataMata config = comTerceiro(jogoUnico(SORTEIO), true);
        List<ParticipanteMotor> participantes = participantes(n);
        Torneio torneio = jogarAteOFim(config, participantes, SEED,
                (fase, confronto, jogo, seeds) -> placar(fase.ordem(), confronto.posicao(), jogo.numeroJogo(), 0, 0));
        List<ResultadoMataMata> embaralhados = new ArrayList<>(torneio.resultados());
        Collections.shuffle(embaralhados, new Random(5));

        assertThat(FORMATO.calcular(config, participantes, embaralhados, SEED)).isEqualTo(torneio.estado());
        assertThat(torneio.estado().campeaoId()).isNotNull();
    }

    static Stream<Integer> tamanhos() {
        return IntStream.rangeClosed(2, 17).boxed();
    }

    // ---------------------------------------------------------------- ninguém aparece duas vezes na fase

    @ParameterizedTest(name = "n={0}, {1}")
    @MethodSource("tamanhosEConfigs")
    void ninguemApareceDuasVezesNaMesmaFaseEOAEhSempreAMelhorSeed(int n, ConfigMataMata config) {
        List<ParticipanteMotor> participantes = participantes(n);
        Torneio torneio = jogarAteOFim(config, participantes, SEED, aleatoria(1000 + n));

        // confere o estado inicial e o estado depois de cada resultado lançado
        for (int quantos = 0; quantos <= torneio.resultados().size(); quantos++) {
            EstadoMataMata estado = FORMATO.calcular(config, participantes, torneio.resultados().subList(0, quantos), SEED);
            for (FaseEstado fase : estado.fases()) {
                assertThat(ocupantes(fase)).as("n=%d, %d resultados, %s", n, quantos, fase.nome()).doesNotHaveDuplicates();
                for (ConfrontoEstado confronto : fase.confrontos()) {
                    if (!confronto.bye() && confronto.participanteAId() != null) {
                        // seeds 1..n com ids 10, 17, ...: a melhor seed tem o menor id
                        assertThat(confronto.participanteAId()).isLessThan(confronto.participanteBId());
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "n={0}, {1}")
    @MethodSource("tamanhosEConfigs")
    void todoTorneioTerminaComCampeaoEVice(int n, ConfigMataMata config) {
        Torneio torneio = jogarAteOFim(config, participantes(n), SEED, aleatoria(2000 + n));

        EstadoMataMata estado = torneio.estado();
        assertThat(estado.campeaoId()).isNotNull();
        assertThat(estado.viceId()).isNotNull().isNotEqualTo(estado.campeaoId());
        if (estado.terceiroId() != null) {
            assertThat(estado.terceiroId()).isNotIn(estado.campeaoId(), estado.viceId());
        }
        // os três do pódio são participantes distintos da chave
        assertThat(estado.fases()).flatExtracting(FaseEstado::confrontos).extracting(ConfrontoEstado::vencedorId)
                .doesNotContainNull();
    }
}
