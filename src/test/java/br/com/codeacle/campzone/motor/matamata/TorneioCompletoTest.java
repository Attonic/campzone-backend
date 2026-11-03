package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.MELHOR_SEED_VENCE;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.PIOR_SEED_VENCE;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.SEED;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comPenaltis;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comTerceiro;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.confronto;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.fase;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idDaSeed;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idaEVolta;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogarAteOFim;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogoUnico;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.ocupantes;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.participantes;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.placar;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.serie;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.codeacle.campzone.motor.matamata.MataMataFixtures.Torneio;

/** Torneios inteiros, lançando todos os resultados até o campeão, com 8 e 11 participantes (com e sem 3º lugar). */
class TorneioCompletoTest {

    /** Nome do tipo, config base e quantos jogos um confronto sem surpresa precisa (1, 2 ou a maioria da série). */
    static Stream<Arguments> tipos() {
        return Stream.of(
                Arguments.of("jogo único", jogoUnico(PENALTIS), 1),
                Arguments.of("ida e volta", idaEVolta(PENALTIS), 2),
                Arguments.of("série melhor de 3", serie(3, PENALTIS), 2),
                Arguments.of("série melhor de 5", serie(5, PENALTIS), 3));
    }

    static Stream<Arguments> cenarios() {
        return cenariosCom(8, 11);
    }

    static Stream<Arguments> cenariosComOito() {
        return cenariosCom(8);
    }

    static Stream<Arguments> cenariosComOnze() {
        return cenariosCom(11);
    }

    private static Stream<Arguments> cenariosCom(int... participantes) {
        return tipos().flatMap(tipo -> Arrays.stream(participantes).boxed().flatMap(n -> Stream.of(false, true)
                .map(terceiro -> Arguments.of(tipo.get()[0], tipo.get()[1], tipo.get()[2], n, terceiro))));
    }

    // ---------------------------------------------------------------- a melhor seed sempre vence

    @ParameterizedTest(name = "{0}, n={3}, terceiro={4}")
    @MethodSource("cenarios")
    void melhorSeedVenceTudoEOPodioSaiNaOrdemDasSeeds(String nome, ConfigMataMata base, int jogosPorConfronto, int n,
            boolean terceiro) {
        Torneio torneio = jogarAteOFim(comTerceiro(base, terceiro), participantes(n), SEED, MELHOR_SEED_VENCE);

        EstadoMataMata estado = torneio.estado();
        assertThat(estado.campeaoId()).isEqualTo(idDaSeed(1));
        assertThat(estado.viceId()).isEqualTo(idDaSeed(2));
        // semifinais 1x4 e 2x3: o 3º lugar é decidido entre o 4 e o 3 (A = 3), e a melhor seed vence
        assertThat(estado.terceiroId()).isEqualTo(terceiro ? idDaSeed(3) : null);
        assertNenhumJogoEmAberto(estado);
        int confrontosComJogo = n - 1 + (terceiro ? 1 : 0);
        assertThat(torneio.resultados()).hasSize(confrontosComJogo * jogosPorConfronto);
    }

    @ParameterizedTest(name = "{0}, n={3}, terceiro={4}")
    @MethodSource("cenarios")
    void umEDoisSoSeEncontramNaFinalEAsSemifinaisSaoUmxQuatroEDoisxTres(String nome, ConfigMataMata base,
            int jogosPorConfronto, int n, boolean terceiro) {
        Torneio torneio = jogarAteOFim(comTerceiro(base, terceiro), participantes(n), SEED, MELHOR_SEED_VENCE);

        EstadoMataMata estado = torneio.estado();
        FaseEstado semifinal = estado.fases().stream().filter(f -> "Semifinal".equals(f.nome())).findFirst().orElseThrow();
        assertThat(semifinal.confrontos()).extracting(c -> List.of(c.participanteAId(), c.participanteBId()))
                .containsExactly(List.of(idDaSeed(1), idDaSeed(4)), List.of(idDaSeed(2), idDaSeed(3)));
        FaseEstado decisao = estado.fases().get(estado.fases().size() - 1);
        assertThat(decisao.tipo()).isEqualTo(TipoFase.FINAL);
        assertThat(decisao.confrontos().get(0).participanteAId()).isEqualTo(idDaSeed(1));
        assertThat(decisao.confrontos().get(0).participanteBId()).isEqualTo(idDaSeed(2));
        // antes da final, 1 e 2 nunca aparecem no mesmo confronto
        for (FaseEstado fase : estado.fases()) {
            if (fase.tipo() != TipoFase.FINAL) {
                for (ConfrontoEstado c : fase.confrontos()) {
                    List<Long> ids = Stream.of(c.participanteAId(), c.participanteBId()).filter(Objects::nonNull).toList();
                    assertThat(ids.contains(idDaSeed(1)) && ids.contains(idDaSeed(2))).as(fase.nome()).isFalse();
                }
            }
        }
    }

    // ---------------------------------------------------------------- só zebras

    @ParameterizedTest(name = "{0}, terceiro={4}")
    @MethodSource("cenariosComOito")
    void zebrasComOitoParticipantes(String nome, ConfigMataMata base, int jogosPorConfronto, int n, boolean terceiro) {
        Torneio torneio = jogarAteOFim(comTerceiro(base, terceiro), participantes(8), SEED, PIOR_SEED_VENCE);

        // quartas: 8, 5, 7 e 6 vencem; semis: 5x8 (8) e 6x7 (7); final: 7x8 (8); 3º: 5x6 (6)
        EstadoMataMata estado = torneio.estado();
        assertThat(estado.campeaoId()).isEqualTo(idDaSeed(8));
        assertThat(estado.viceId()).isEqualTo(idDaSeed(7));
        assertThat(estado.terceiroId()).isEqualTo(terceiro ? idDaSeed(6) : null);
        assertNenhumJogoEmAberto(estado);
    }

    @ParameterizedTest(name = "{0}, terceiro={4}")
    @MethodSource("cenariosComOnze")
    void zebrasComOnzeParticipantes(String nome, ConfigMataMata base, int jogosPorConfronto, int n, boolean terceiro) {
        Torneio torneio = jogarAteOFim(comTerceiro(base, terceiro), participantes(11), SEED, PIOR_SEED_VENCE);

        // oitavas (com byes): 1, 9, 4, 5, 2, 10, 3, 11; quartas: 9, 5, 10, 11; semis: 9 e 11; final: 11; 3º: 5x10 (10)
        EstadoMataMata estado = torneio.estado();
        assertThat(estado.campeaoId()).isEqualTo(idDaSeed(11));
        assertThat(estado.viceId()).isEqualTo(idDaSeed(9));
        assertThat(estado.terceiroId()).isEqualTo(terceiro ? idDaSeed(10) : null);
        assertNenhumJogoEmAberto(estado);
    }

    @ParameterizedTest(name = "{0}, n={3}, terceiro={4}")
    @MethodSource("cenarios")
    void comZebrasOAEAindaAMelhorSeedENinguemRepeteNaFase(String nome, ConfigMataMata base, int jogosPorConfronto,
            int n, boolean terceiro) {
        Torneio torneio = jogarAteOFim(comTerceiro(base, terceiro), participantes(n), SEED, PIOR_SEED_VENCE);

        for (FaseEstado fase : torneio.estado().fases()) {
            assertThat(ocupantes(fase)).as(fase.nome()).doesNotHaveDuplicates();
            assertThat(fase.confrontos()).filteredOn(c -> !c.bye()).allSatisfy(c -> assertThat(c.participanteAId())
                    .isLessThan(c.participanteBId()));
        }
    }

    // ---------------------------------------------------------------- decidido nos pênaltis

    @ParameterizedTest(name = "{0}, n={3}, terceiro={4}")
    @MethodSource("cenarios")
    void todoJogoEmpatadoDecididoNosPenaltis(String nome, ConfigMataMata base, int jogosPorConfronto, int n,
            boolean terceiro) {
        MataMataFixtures.Politica empateEPenaltis = (fase, confronto, jogo, seeds) -> {
            boolean idaDaIdaEVolta = confronto.jogos().size() == 2 && jogo.numeroJogo() == 1;
            if (idaDaIdaEVolta) {
                return placar(fase.ordem(), confronto.posicao(), 1, 1, 1);
            }
            boolean mandanteMelhor = seeds.get(jogo.mandanteId()) < seeds.get(jogo.visitanteId());
            return comPenaltis(fase.ordem(), confronto.posicao(), jogo.numeroJogo(), 1, 1, mandanteMelhor ? 5 : 4,
                    mandanteMelhor ? 4 : 5);
        };

        Torneio torneio = jogarAteOFim(comTerceiro(base, terceiro), participantes(n), SEED, empateEPenaltis);

        // a melhor seed vence todos os pênaltis: mesmo pódio do torneio sem surpresas
        assertThat(torneio.estado().campeaoId()).isEqualTo(idDaSeed(1));
        assertThat(torneio.estado().viceId()).isEqualTo(idDaSeed(2));
        assertThat(torneio.estado().terceiroId()).isEqualTo(terceiro ? idDaSeed(3) : null);
        assertNenhumJogoEmAberto(torneio.estado());
    }

    // ---------------------------------------------------------------- campeonato de quatro com ida e volta

    @Test
    void quatroParticipantesIdaEVoltaAteOPodio() {
        ConfigMataMata config = comTerceiro(idaEVolta(PENALTIS), true);

        Torneio torneio = jogarAteOFim(config, participantes(4), SEED, MELHOR_SEED_VENCE);

        EstadoMataMata estado = torneio.estado();
        assertThat(estado.fases()).extracting(FaseEstado::nome).containsExactly("Semifinal", "Disputa de 3º lugar", "Final");
        assertThat(estado.campeaoId()).isEqualTo(idDaSeed(1));
        assertThat(estado.viceId()).isEqualTo(idDaSeed(2));
        assertThat(estado.terceiroId()).isEqualTo(idDaSeed(3));
        // semifinais 1x4 e 2x3, disputa 3x4 e final 1x2: 4 confrontos, 2 jogos cada
        assertThat(torneio.resultados()).hasSize(8);
        assertThat(confronto(estado, 3, 1).jogos()).extracting(JogoEstado::mandanteId)
                .containsExactly(idDaSeed(2), idDaSeed(1));
    }

    // ---------------------------------------------------------------- auxiliar

    private static void assertNenhumJogoEmAberto(EstadoMataMata estado) {
        assertThat(estado.fases()).flatExtracting(FaseEstado::confrontos).flatExtracting(ConfrontoEstado::jogos)
                .extracting(JogoEstado::situacao)
                .doesNotContain(SituacaoJogo.PENDENTE, SituacaoJogo.AGUARDANDO_PARTICIPANTES);
        assertThat(estado.fases()).flatExtracting(FaseEstado::confrontos).extracting(ConfrontoEstado::vencedorId)
                .doesNotContainNull();
        assertThat(fase(estado, estado.fases().size()).tipo()).isEqualTo(TipoFase.FINAL);
    }
}
