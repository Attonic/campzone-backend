package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.GOLS_FORA;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.SORTEIO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.FORMATO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comPenaltis;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.linhas;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.participantes;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.placar;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesDeTeste;

/**
 * Trava o estado byte a byte: o algoritmo de {@code Random(seed)} e de {@code Collections.shuffle} é fixado em
 * contrato pelo JDK, então estas chaves não podem mudar entre versões nem execuções. Cada linha é um confronto, com
 * os ids de A e B, o vencedor e os jogos como {@code J<n>:<mandante>x<visitante>:<situação>}.
 */
class GoldenMataMataTest {

    /** Com a seed 42, a ordem sorteada dos ids 10, 17, 24, 31, 38 é [17, 24, 31, 38, 10], logo seeds 1 a 5. */
    @Test
    void cincoParticipantesSeedingAleatorioComSeed42() {
        ConfigMataMata config = new ConfigMataMata(TipoConfronto.JOGO_UNICO, null, List.of(PENALTIS), Seeding.ALEATORIO,
                false);

        EstadoMataMata estado = FORMATO.gerar(config, ParticipantesDeTeste.com(5), 42L);

        assertThat(linhas(estado)).containsExactly(
                "F1 Quartas de final ELIMINATORIA",
                "  C1 17-- BYE vence=17",
                "  C2 38-10 vence=- | J1:38x10:PENDENTE",
                "  C3 24-- BYE vence=24",
                "  C4 31-- BYE vence=31",
                "F2 Semifinal ELIMINATORIA",
                "  C1 --- vence=- | J1:-x-:AGUARDANDO_PARTICIPANTES",
                "  C2 --- vence=- | J1:-x-:AGUARDANDO_PARTICIPANTES",
                "F3 Final FINAL",
                "  C1 --- vence=- | J1:-x-:AGUARDANDO_PARTICIPANTES",
                "campeao=- vice=- terceiro=-");
    }

    /**
     * Cabeças de chave com k = 2: a seed 1 é o id 24 e a 2 é o id 10. Os ids sem seed (17, 31, 38) são embaralhados com
     * a seed 42 e dão [31, 17, 38], ou seja, seeds 3, 4 e 5.
     */
    @Test
    void cincoParticipantesCabecasDeChaveComDoisCabecasESeed42() {
        ConfigMataMata config = new ConfigMataMata(TipoConfronto.JOGO_UNICO, null, List.of(PENALTIS),
                Seeding.CABECAS_DE_CHAVE, true);
        List<ParticipanteMotor> participantes = ParticipantesDeTeste.comSeeds(2, null, 1, null, null);

        EstadoMataMata estado = FORMATO.gerar(config, participantes, 42L);

        assertThat(linhas(estado)).containsExactly(
                "F1 Quartas de final ELIMINATORIA",
                "  C1 24-- BYE vence=24",
                "  C2 17-38 vence=- | J1:17x38:PENDENTE",
                "  C3 10-- BYE vence=10",
                "  C4 31-- BYE vence=31",
                "F2 Semifinal ELIMINATORIA",
                "  C1 --- vence=- | J1:-x-:AGUARDANDO_PARTICIPANTES",
                "  C2 --- vence=- | J1:-x-:AGUARDANDO_PARTICIPANTES",
                "F3 Disputa de 3º lugar TERCEIRO_LUGAR",
                "  C1 --- vence=- | J1:-x-:AGUARDANDO_PARTICIPANTES",
                "F4 Final FINAL",
                "  C1 --- vence=- | J1:-x-:AGUARDANDO_PARTICIPANTES",
                "campeao=- vice=- terceiro=-");
    }

    /** Torneio de 4 participantes, ida e volta com gols fora e pênaltis, com 3º lugar, resolvido até o pódio. */
    @Test
    void quatroParticipantesIdaEVoltaAtePodio() {
        ConfigMataMata config = new ConfigMataMata(TipoConfronto.IDA_E_VOLTA, null, List.of(GOLS_FORA, PENALTIS),
                Seeding.MANUAL, true);
        List<ParticipanteMotor> participantes = participantes(4);
        List<ResultadoMataMata> resultados = List.of(
                // semi 1 (10 x 31): ida 31 1x1 10, volta 10 2x0 31 -> 10 vence por 3 x 1
                placar(1, 1, 1, 1, 1), placar(1, 1, 2, 2, 0),
                // semi 2 (17 x 24): ida 24 2x1 17, volta 17 1x0 24 -> 2 x 2, gols fora 1 x 0 -> 17 vence
                placar(1, 2, 1, 2, 1), placar(1, 2, 2, 1, 0),
                // 3º lugar (24 x 31): 0 x 0 nas duas partidas, gols fora 0 x 0, pênaltis 3 x 4 -> 31 vence
                placar(2, 1, 1, 0, 0), comPenaltis(2, 1, 2, 0, 0, 3, 4),
                // final (10 x 17): ida 17 2x0 10, volta 10 3x0 17 -> 10 vence por 3 x 2
                placar(3, 1, 1, 2, 0), placar(3, 1, 2, 3, 0));

        assertThat(linhas(FORMATO.calcular(config, participantes, resultados.subList(0, 4), 1L))).containsExactly(
                "F1 Semifinal ELIMINATORIA",
                "  C1 10-31 vence=10 | J1:31x10:DISPUTADO J2:10x31:DISPUTADO",
                "  C2 17-24 vence=17 | J1:24x17:DISPUTADO J2:17x24:DISPUTADO",
                "F2 Disputa de 3º lugar TERCEIRO_LUGAR",
                "  C1 24-31 vence=- | J1:31x24:PENDENTE J2:24x31:PENDENTE",
                "F3 Final FINAL",
                "  C1 10-17 vence=- | J1:17x10:PENDENTE J2:10x17:PENDENTE",
                "campeao=- vice=- terceiro=-");
        assertThat(linhas(FORMATO.calcular(config, participantes, resultados, 1L))).containsExactly(
                "F1 Semifinal ELIMINATORIA",
                "  C1 10-31 vence=10 | J1:31x10:DISPUTADO J2:10x31:DISPUTADO",
                "  C2 17-24 vence=17 | J1:24x17:DISPUTADO J2:17x24:DISPUTADO",
                "F2 Disputa de 3º lugar TERCEIRO_LUGAR",
                "  C1 24-31 vence=31 | J1:31x24:DISPUTADO J2:24x31:DISPUTADO",
                "F3 Final FINAL",
                "  C1 10-17 vence=10 | J1:17x10:DISPUTADO J2:10x17:DISPUTADO",
                "campeao=10 vice=17 terceiro=31");
    }

    /**
     * Quatro participantes, todo jogo 0 x 0 decidido pelo sorteio, com a seed 42. Pela fórmula do ADR (SplitMix64), os
     * sorteios dão A nas semifinais 1 e 2 (fase 1), na disputa de 3º (fase 2) e na final (fase 3). Os perdedores das
     * semifinais são as seeds 4 e 3, e o A da disputa é a seed 3 (id 24).
     */
    @Test
    void quatroParticipantesDecididosNoSorteioComSeed42() {
        ConfigMataMata config = new ConfigMataMata(TipoConfronto.JOGO_UNICO, null, List.of(SORTEIO), Seeding.MANUAL,
                true);
        List<ParticipanteMotor> participantes = participantes(4);

        MataMataFixtures.Torneio torneio = MataMataFixtures.jogarAteOFim(config, participantes, 42L,
                (fase, confronto, jogo, seeds) -> placar(fase.ordem(), confronto.posicao(), jogo.numeroJogo(), 0, 0));

        assertThat(linhas(torneio.estado())).containsExactly(
                "F1 Semifinal ELIMINATORIA",
                "  C1 10-31 vence=10 | J1:10x31:DISPUTADO",
                "  C2 17-24 vence=17 | J1:17x24:DISPUTADO",
                "F2 Disputa de 3º lugar TERCEIRO_LUGAR",
                "  C1 24-31 vence=24 | J1:24x31:DISPUTADO",
                "F3 Final FINAL",
                "  C1 10-17 vence=10 | J1:10x17:DISPUTADO",
                "campeao=10 vice=17 terceiro=24");
    }
}
