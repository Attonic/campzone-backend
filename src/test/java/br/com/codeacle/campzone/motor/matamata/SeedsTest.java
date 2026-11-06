package br.com.codeacle.campzone.motor.matamata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.motor.OrdemSorteada;
import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesDeTeste;
import br.com.codeacle.campzone.motor.ParticipantesInvalidosException;

/** Atribuição das seeds de 1 a n em cada tipo de seeding. Os ids de teste são 10, 17, 24, 31, 38... */
class SeedsTest {

    private static final long SEED = 20260;

    // ---------------------------------------------------------------- ALEATORIO

    @Test
    void aleatorioUsaAOrdemSorteadaDosIds() {
        List<ParticipanteMotor> participantes = ParticipantesDeTeste.com(6);

        assertThat(Seeds.porSeed(Seeding.ALEATORIO, participantes, SEED))
                .isEqualTo(OrdemSorteada.de(participantes, SEED));
    }

    @Test
    void aleatorioIgnoraAsSeedsInformadasMesmoInvalidas() {
        List<ParticipanteMotor> semSeeds = ParticipantesDeTeste.com(5);
        List<ParticipanteMotor> comSeedsAbsurdas = ParticipantesDeTeste.comSeeds(5, 5, -1, 0, 99);

        assertThat(Seeds.porSeed(Seeding.ALEATORIO, comSeedsAbsurdas, SEED))
                .isEqualTo(Seeds.porSeed(Seeding.ALEATORIO, semSeeds, SEED));
    }

    @Test
    void aleatorioNaoDependeDaOrdemDeEntrada() {
        List<ParticipanteMotor> embaralhados = new ArrayList<>(ParticipantesDeTeste.com(8));
        Collections.shuffle(embaralhados, new Random(3));

        assertThat(Seeds.porSeed(Seeding.ALEATORIO, embaralhados, SEED))
                .isEqualTo(Seeds.porSeed(Seeding.ALEATORIO, ParticipantesDeTeste.com(8), SEED));
    }

    // ---------------------------------------------------------------- MANUAL

    @Test
    void manualUsaAsSeedsInformadas() {
        // participante 0 (id 10) é a seed 3; o 1 (id 17) é a 1; o 2 (id 24) é a 4; o 3 (id 31) é a 2
        List<ParticipanteMotor> participantes = ParticipantesDeTeste.comSeeds(3, 1, 4, 2);

        assertThat(Seeds.porSeed(Seeding.MANUAL, participantes, SEED)).containsExactly(17L, 31L, 10L, 24L);
    }

    @Test
    void manualNaoDependeDaSeedDeSorteio() {
        List<ParticipanteMotor> participantes = ParticipantesDeTeste.comSeeds(2, 1, 3);

        assertThat(Seeds.porSeed(Seeding.MANUAL, participantes, 1L))
                .isEqualTo(Seeds.porSeed(Seeding.MANUAL, participantes, 999L));
    }

    @Test
    void manualRecusaParticipanteSemSeed() {
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.MANUAL, ParticipantesDeTeste.comSeeds(1, null, 3), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("MANUAL")
                .hasMessageContaining("17");
    }

    @Test
    void manualRecusaSeedRepetida() {
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.MANUAL, ParticipantesDeTeste.comSeeds(1, 2, 2), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("repetida")
                .hasMessageContaining("2");
    }

    @Test
    void manualRecusaSeedForaDeUmAN() {
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.MANUAL, ParticipantesDeTeste.comSeeds(1, 2, 4), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("seed 4")
                .hasMessageContaining("1 a 3");
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.MANUAL, ParticipantesDeTeste.comSeeds(0, 1, 2), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("seed 0");
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.MANUAL, ParticipantesDeTeste.comSeeds(-1, 1, 2), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class);
    }

    // ---------------------------------------------------------------- CABECAS_DE_CHAVE

    @Test
    void cabecasComKZeroSeComportaComoAleatorio() {
        List<ParticipanteMotor> participantes = ParticipantesDeTeste.com(6);

        assertThat(Seeds.porSeed(Seeding.CABECAS_DE_CHAVE, participantes, SEED))
                .isEqualTo(Seeds.porSeed(Seeding.ALEATORIO, participantes, SEED));
    }

    @Test
    void cabecasComDoisFixamAsDuasPrimeirasSeedsESorteiamOResto() {
        // ids: 10 (seed 2), 17 (sem), 24 (seed 1), 31 (sem), 38 (sem)
        List<ParticipanteMotor> participantes = ParticipantesDeTeste.comSeeds(2, null, 1, null, null);

        List<Long> porSeed = Seeds.porSeed(Seeding.CABECAS_DE_CHAVE, participantes, SEED);

        assertThat(porSeed.subList(0, 2)).containsExactly(24L, 10L);
        // o resto é o sorteio só entre os ids sem seed, ordenados, com Random(seedSorteio)
        List<Long> restoSorteado = OrdemSorteada.de(List.of(participantes.get(1), participantes.get(3),
                participantes.get(4)), SEED);
        assertThat(porSeed.subList(2, 5)).isEqualTo(restoSorteado);
    }

    @Test
    void cabecasComTodosComSeedEquivaleAoManual() {
        List<ParticipanteMotor> participantes = ParticipantesDeTeste.comSeeds(3, 1, 2);

        assertThat(Seeds.porSeed(Seeding.CABECAS_DE_CHAVE, participantes, SEED)).containsExactly(17L, 24L, 10L);
        assertThat(Seeds.porSeed(Seeding.CABECAS_DE_CHAVE, participantes, SEED))
                .isEqualTo(Seeds.porSeed(Seeding.MANUAL, participantes, SEED));
    }

    @Test
    void cabecasNaoDependeDaOrdemDeEntrada() {
        List<ParticipanteMotor> participantes = new ArrayList<>(ParticipantesDeTeste.comSeeds(1, null, null, 2, null, null));
        List<ParticipanteMotor> embaralhados = new ArrayList<>(participantes);
        Collections.shuffle(embaralhados, new Random(11));

        assertThat(Seeds.porSeed(Seeding.CABECAS_DE_CHAVE, embaralhados, SEED))
                .isEqualTo(Seeds.porSeed(Seeding.CABECAS_DE_CHAVE, participantes, SEED));
    }

    @Test
    void cabecasRecusaSeedsComBuraco() {
        // k = 3 cabeças com seeds 1, 2 e 4: falta a 3
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.CABECAS_DE_CHAVE,
                ParticipantesDeTeste.comSeeds(1, 2, 4, null, null), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("seed 4")
                .hasMessageContaining("1..3")
                .hasMessageContaining("sem buracos");
    }

    @Test
    void cabecasRecusaSeedsQueNaoComecamEmUm() {
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.CABECAS_DE_CHAVE,
                ParticipantesDeTeste.comSeeds(2, 3, null, null), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("seed 3");
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.CABECAS_DE_CHAVE,
                ParticipantesDeTeste.comSeeds(0, 1, null, null), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("seed 0");
    }

    @Test
    void cabecasRecusaSeedRepetida() {
        assertThatThrownBy(() -> Seeds.porSeed(Seeding.CABECAS_DE_CHAVE,
                ParticipantesDeTeste.comSeeds(1, 1, null, null), SEED))
                .isInstanceOf(ParticipantesInvalidosException.class)
                .hasMessageContaining("repetida");
    }

    @Test
    void cabecasAceitaUmUnicoCabecaDeChave() {
        assertThatCode(() -> Seeds.porSeed(Seeding.CABECAS_DE_CHAVE,
                ParticipantesDeTeste.comSeeds(null, 1, null, null), SEED)).doesNotThrowAnyException();
        assertThat(Seeds.porSeed(Seeding.CABECAS_DE_CHAVE, ParticipantesDeTeste.comSeeds(null, 1, null, null), SEED)
                .get(0)).isEqualTo(17L);
    }
}
