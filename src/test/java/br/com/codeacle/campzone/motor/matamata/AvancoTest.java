package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.MELHOR_SEED_VENCE;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.SEED;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.calcular;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comTerceiro;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.confronto;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.fase;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idDaSeed;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idaEVolta;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogoUnico;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.participantes;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.placar;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.situacoes;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.codeacle.campzone.motor.ParticipanteMotor;

/** Avanço de fase, posição dos vencedores, disputa de 3º lugar e pódio. Jogo único, seeds 1..n (MANUAL). */
class AvancoTest {

    private final ConfigMataMata semTerceiro = jogoUnico(PENALTIS);
    private final ConfigMataMata comTerceiro = comTerceiro(semTerceiro, true);

    /** Vence o mandante (o A, a melhor seed) por 1 x 0. */
    private static ResultadoMataMata venceA(int fase, int confronto) {
        return placar(fase, confronto, 1, 1, 0);
    }

    /** Vence o visitante (o B, a pior seed) por 0 x 1. */
    private static ResultadoMataMata venceB(int fase, int confronto) {
        return placar(fase, confronto, 1, 0, 1);
    }

    // ---------------------------------------------------------------- a fase só abre com a anterior completa

    @Test
    void faseSeguinteSoAbreQuandoTodosOsConfrontosDaAnteriorTemVencedor() {
        // n = 8: quartas 1x8, 4x5, 2x7, 3x6. Com 3 das 4 decididas, nada muda nas semifinais
        EstadoMataMata tresDeQuatro = calcular(semTerceiro, 8, venceA(1, 1), venceA(1, 2), venceA(1, 3));

        assertThat(fase(tresDeQuatro, 2).confrontos()).allSatisfy(c -> {
            assertThat(c.participanteAId()).isNull();
            assertThat(c.participanteBId()).isNull();
            assertThat(c.vencedorId()).isNull();
            assertThat(c.jogos()).extracting(JogoEstado::situacao).containsOnly(SituacaoJogo.AGUARDANDO_PARTICIPANTES);
            assertThat(c.jogos()).allSatisfy(j -> {
                assertThat(j.mandanteId()).isNull();
                assertThat(j.visitanteId()).isNull();
            });
        });

        EstadoMataMata todas = calcular(semTerceiro, 8, venceA(1, 1), venceA(1, 2), venceA(1, 3), venceA(1, 4));

        assertThat(fase(todas, 2).confrontos()).allSatisfy(c -> {
            assertThat(c.participanteAId()).isNotNull();
            assertThat(c.participanteBId()).isNotNull();
            assertThat(c.jogos()).extracting(JogoEstado::situacao).containsOnly(SituacaoJogo.PENDENTE);
        });
    }

    @Test
    void comByesAPrimeiraFaseTambemPrecisaFecharPorInteiro() {
        // n = 5, chave de 8: byes para as seeds 1, 2 e 3; só o 4x5 tem jogo. Enquanto ele não termina, as semis esperam
        EstadoMataMata aberta = calcular(semTerceiro, 5);
        EstadoMataMata fechada = calcular(semTerceiro, 5, venceA(1, 2));

        assertThat(confronto(aberta, 2, 1).participanteAId()).isNull();
        assertThat(confronto(aberta, 2, 2).participanteAId()).isNull();
        // semi 1: vencedor do 1x8 (seed 1, bye) contra o vencedor do 4x5 (seed 4); semi 2: seed 2 contra seed 3
        assertThat(confronto(fechada, 2, 1).participanteAId()).isEqualTo(idDaSeed(1));
        assertThat(confronto(fechada, 2, 1).participanteBId()).isEqualTo(idDaSeed(4));
        assertThat(confronto(fechada, 2, 2).participanteAId()).isEqualTo(idDaSeed(2));
        assertThat(confronto(fechada, 2, 2).participanteBId()).isEqualTo(idDaSeed(3));
    }

    @Test
    void duasSeedsComByeSoSeEnfrentamQuandoAFaseAnteriorFecha() {
        // n = 3: semi 1 é bye da seed 1; semi 2 é 2x3. A final só abre quando a 2x3 termina
        EstadoMataMata esperando = calcular(semTerceiro, 3);
        EstadoMataMata aberta = calcular(semTerceiro, 3, venceB(1, 2));

        assertThat(confronto(esperando, 2, 1).participanteAId()).isNull();
        assertThat(situacoes(confronto(esperando, 2, 1))).containsExactly(SituacaoJogo.AGUARDANDO_PARTICIPANTES);
        // 3 vence a seed 2 (B vence): final com a seed 1 (A, bye) contra a seed 3
        assertThat(confronto(aberta, 2, 1).participanteAId()).isEqualTo(idDaSeed(1));
        assertThat(confronto(aberta, 2, 1).participanteBId()).isEqualTo(idDaSeed(3));
        assertThat(situacoes(confronto(aberta, 2, 1))).containsExactly(SituacaoJogo.PENDENTE);
    }

    // ---------------------------------------------------------------- posição dos vencedores

    @Test
    void vencedorDaPosicaoPVaiParaAPosicaoCeilDePPor2ComAMelhorSeedComoA() {
        // quartas: 1x8 (8 vence), 4x5 (4 vence), 2x7 (2 vence), 3x6 (6 vence)
        EstadoMataMata estado = calcular(semTerceiro, 8, venceB(1, 1), venceA(1, 2), venceA(1, 3), venceB(1, 4));

        // semi 1 recebe os vencedores das posições 1 e 2 (seeds 8 e 4): A é a seed 4, a melhor
        assertThat(confronto(estado, 2, 1).participanteAId()).isEqualTo(idDaSeed(4));
        assertThat(confronto(estado, 2, 1).participanteBId()).isEqualTo(idDaSeed(8));
        // semi 2 recebe os vencedores das posições 3 e 4 (seeds 2 e 6): A é a seed 2
        assertThat(confronto(estado, 2, 2).participanteAId()).isEqualTo(idDaSeed(2));
        assertThat(confronto(estado, 2, 2).participanteBId()).isEqualTo(idDaSeed(6));
        // o A manda o jogo
        assertThat(confronto(estado, 2, 1).jogos().get(0).mandanteId()).isEqualTo(idDaSeed(4));
    }

    @Test
    void vencedorDePosicaoParTambemPodeSerOAQuandoTemAMelhorSeed() {
        // 4x5: 5 vence; 1x8: 1 vence. Semi 1: seed 1 (posição 1, ímpar) e seed 5 (posição 2, par)
        EstadoMataMata estado = calcular(semTerceiro, 8, venceA(1, 1), venceB(1, 2), venceA(1, 3), venceA(1, 4));

        assertThat(confronto(estado, 2, 1).participanteAId()).isEqualTo(idDaSeed(1));
        assertThat(confronto(estado, 2, 1).participanteBId()).isEqualTo(idDaSeed(5));
    }

    @Test
    void zebrasNasFasesSeguintesTambemReordenamPelaSeed() {
        EstadoMataMata estado = jogarSemifinaisComZebras(semTerceiro);

        // final: vencedor da semi 1 (4x8 -> 8 vence) e da semi 2 (2x6 -> 2 vence); A é a seed 2
        assertThat(confronto(estado, 3, 1).participanteAId()).isEqualTo(idDaSeed(2));
        assertThat(confronto(estado, 3, 1).participanteBId()).isEqualTo(idDaSeed(8));
    }

    private EstadoMataMata jogarSemifinaisComZebras(ConfigMataMata config) {
        // quartas: 8, 4, 2, 6 vencem; semis: 4x8 (8 vence) e 2x6 (2 vence)
        int semi = 2;
        return calcular(config, 8, venceB(1, 1), venceA(1, 2), venceA(1, 3), venceB(1, 4), venceB(semi, 1),
                venceA(semi, 2));
    }

    // ---------------------------------------------------------------- disputa de 3º lugar

    @Test
    void semAConfigNaoHaDisputaDeTerceiro() {
        EstadoMataMata estado = calcular(semTerceiro, 8);

        assertThat(estado.fases()).extracting(FaseEstado::nome)
                .containsExactly("Quartas de final", "Semifinal", "Final");
        assertThat(estado.fases()).extracting(FaseEstado::tipo)
                .containsExactly(TipoFase.ELIMINATORIA, TipoFase.ELIMINATORIA, TipoFase.FINAL);
        assertThat(estado.fases()).extracting(FaseEstado::ordem).containsExactly(1, 2, 3);
    }

    @Test
    void comAConfigADisputaTemOrdemKEAFinalFicaComKMaisUm() {
        EstadoMataMata estado = calcular(comTerceiro, 8);

        assertThat(estado.fases()).extracting(FaseEstado::nome)
                .containsExactly("Quartas de final", "Semifinal", "Disputa de 3º lugar", "Final");
        assertThat(estado.fases()).extracting(FaseEstado::tipo).containsExactly(TipoFase.ELIMINATORIA,
                TipoFase.ELIMINATORIA, TipoFase.TERCEIRO_LUGAR, TipoFase.FINAL);
        assertThat(estado.fases()).extracting(FaseEstado::ordem).containsExactly(1, 2, 3, 4);
        assertThat(fase(estado, 3).confrontos()).hasSize(1);
        assertThat(fase(estado, 4).confrontos()).hasSize(1);
    }

    @Test
    void disputaDeTerceiroEFinalAbremJuntasQuandoAsSemifinaisFecham() {
        // semi 1 decidida (4x... ), semi 2 ainda não: nenhuma das duas abre
        EstadoMataMata umaSemi = calcular(comTerceiro, 8, venceA(1, 1), venceA(1, 2), venceA(1, 3), venceA(1, 4),
                venceA(2, 1));
        assertThat(confronto(umaSemi, 3, 1).participanteAId()).isNull();
        assertThat(situacoes(confronto(umaSemi, 3, 1))).containsExactly(SituacaoJogo.AGUARDANDO_PARTICIPANTES);
        assertThat(confronto(umaSemi, 4, 1).participanteAId()).isNull();
        assertThat(situacoes(confronto(umaSemi, 4, 1))).containsExactly(SituacaoJogo.AGUARDANDO_PARTICIPANTES);

        EstadoMataMata duasSemis = calcular(comTerceiro, 8, venceA(1, 1), venceA(1, 2), venceA(1, 3), venceA(1, 4),
                venceA(2, 1), venceA(2, 2));
        assertThat(situacoes(confronto(duasSemis, 3, 1))).containsExactly(SituacaoJogo.PENDENTE);
        assertThat(situacoes(confronto(duasSemis, 4, 1))).containsExactly(SituacaoJogo.PENDENTE);
    }

    @Test
    void disputaDeTerceiroReuneOsPerdedoresDasSemifinaisComAMelhorSeedComoA() {
        // quartas pelo favorito: semi 1 = 1x4, semi 2 = 2x3. Semi 1: 4 vence; semi 2: 2 vence
        EstadoMataMata estado = calcular(comTerceiro, 8, venceA(1, 1), venceA(1, 2), venceA(1, 3), venceA(1, 4),
                venceB(2, 1), venceA(2, 2));

        // perdedores: seed 1 (semi 1) e seed 3 (semi 2). Finalistas: seeds 4 e 2
        ConfrontoEstado terceiro = confronto(estado, 3, 1);
        assertThat(terceiro.participanteAId()).isEqualTo(idDaSeed(1));
        assertThat(terceiro.participanteBId()).isEqualTo(idDaSeed(3));
        assertThat(terceiro.posicao()).isEqualTo(1);
        assertThat(terceiro.jogos().get(0).mandanteId()).isEqualTo(idDaSeed(1));
        ConfrontoEstado decisao = confronto(estado, 4, 1);
        assertThat(decisao.participanteAId()).isEqualTo(idDaSeed(2));
        assertThat(decisao.participanteBId()).isEqualTo(idDaSeed(4));
    }

    @Test
    void disputaDeTerceiroSegueOTipoDeConfrontoDaConfig() {
        ConfigMataMata ida = comTerceiro(idaEVolta(PENALTIS), true);
        var torneio = MataMataFixtures.jogarAteOFim(ida, participantes(4), SEED, MELHOR_SEED_VENCE);

        ConfrontoEstado terceiro = confronto(torneio.estado(), 2, 1);
        assertThat(terceiro.jogos()).hasSize(2);
        assertThat(terceiro.jogos()).extracting(JogoEstado::situacao).containsOnly(SituacaoJogo.DISPUTADO);
    }

    @ParameterizedTest(name = "n={0}")
    @ValueSource(ints = { 4, 5, 6, 7, 8, 9, 11, 16 })
    void disputaDeTerceiroExisteQuandoAsSemifinaisNaoTemBye(int n) {
        EstadoMataMata estado = calcular(comTerceiro, n);

        assertThat(estado.fases()).extracting(FaseEstado::tipo).contains(TipoFase.TERCEIRO_LUGAR);
        assertThat(estado.fases().get(estado.fases().size() - 1).tipo()).isEqualTo(TipoFase.FINAL);
        assertThat(estado.fases().get(estado.fases().size() - 2).tipo()).isEqualTo(TipoFase.TERCEIRO_LUGAR);
    }

    @Test
    void comTresParticipantesADisputaDeTerceiroNaoEGerada() {
        EstadoMataMata estado = calcular(comTerceiro, 3);

        assertThat(estado.fases()).extracting(FaseEstado::nome).containsExactly("Semifinal", "Final");
        assertThat(estado.fases()).extracting(FaseEstado::tipo).doesNotContain(TipoFase.TERCEIRO_LUGAR);
        assertThat(estado.fases()).extracting(FaseEstado::ordem).containsExactly(1, 2);
    }

    @Test
    void comDoisParticipantesNaoHaSemifinalNemDisputaDeTerceiro() {
        EstadoMataMata estado = calcular(comTerceiro, 2);

        assertThat(estado.fases()).extracting(FaseEstado::tipo).containsExactly(TipoFase.FINAL);
    }

    @Test
    void comTresParticipantesOTerceiroFicaNuloAteNoFimDoTorneio() {
        var torneio = MataMataFixtures.jogarAteOFim(comTerceiro, participantes(3), SEED, MELHOR_SEED_VENCE);

        assertThat(torneio.estado().campeaoId()).isEqualTo(idDaSeed(1));
        assertThat(torneio.estado().viceId()).isEqualTo(idDaSeed(2));
        assertThat(torneio.estado().terceiroId()).isNull();
    }

    // ---------------------------------------------------------------- o A é a melhor seed, não o menor id

    /**
     * Ids que caem com a seed (a seed {@code s} é o id {@code 1000 - 7 s}): a melhor seed tem o MAIOR id. Escolher o A
     * pelo menor id, em vez da melhor seed, inverteria A e B em todas as fases depois da primeira.
     */
    @Test
    void depoisDaPrimeiraFaseOAEAMelhorSeedMesmoComIdMaior() {
        List<ParticipanteMotor> participantes = MataMataFixtures.participantesComIdInvertido(8);
        // quartas por zebras e favoritos: 8 vence o 1; 4 vence o 5; 2 vence o 7; 6 vence o 3
        // semifinais: 4 x 8 (4 vence) e 2 x 6 (6 vence)
        EstadoMataMata estado = MataMataFixtures.calcularCom(comTerceiro, participantes, venceB(1, 1), venceA(1, 2),
                venceA(1, 3), venceB(1, 4), venceA(2, 1), venceB(2, 2));

        long seed2 = idInvertido(2);
        long seed4 = idInvertido(4);
        long seed6 = idInvertido(6);
        long seed8 = idInvertido(8);
        // semifinais: A é a seed 4 (id 972) contra a 8 (id 944), e a seed 2 (id 986) contra a 6 (id 958)
        assertThat(confronto(estado, 2, 1).participanteAId()).isEqualTo(seed4).isGreaterThan(seed8);
        assertThat(confronto(estado, 2, 1).participanteBId()).isEqualTo(seed8);
        assertThat(confronto(estado, 2, 2).participanteAId()).isEqualTo(seed2).isGreaterThan(seed6);
        assertThat(confronto(estado, 2, 2).participanteBId()).isEqualTo(seed6);
        // final: 4 (A, id 972) contra 6 (B, id 958); o A manda o jogo
        ConfrontoEstado decisao = confronto(estado, 4, 1);
        assertThat(decisao.participanteAId()).isEqualTo(seed4).isGreaterThan(seed6);
        assertThat(decisao.participanteBId()).isEqualTo(seed6);
        assertThat(decisao.jogos().get(0).mandanteId()).isEqualTo(seed4);
        // disputa de 3º: perdedores 8 (semi 1) e 2 (semi 2); o A é a seed 2 (id 986), que tem o id maior
        ConfrontoEstado terceiro = confronto(estado, 3, 1);
        assertThat(terceiro.participanteAId()).isEqualTo(seed2).isGreaterThan(seed8);
        assertThat(terceiro.participanteBId()).isEqualTo(seed8);
        assertThat(terceiro.jogos().get(0).mandanteId()).isEqualTo(seed2);
    }

    private static long idInvertido(int seed) {
        return MataMataFixtures.idInvertidoDaSeed(seed);
    }

    // ---------------------------------------------------------------- pódio

    @Test
    void podioSoSePreencheAoDecidirAFinalEADisputaDeTerceiro() {
        List<ResultadoMataMata> ate = List.of(venceA(1, 1), venceA(1, 2), venceA(1, 3), venceA(1, 4), venceA(2, 1),
                venceA(2, 2));

        EstadoMataMata semFinal = formato(comTerceiro, ate);
        assertThat(semFinal.campeaoId()).isNull();
        assertThat(semFinal.viceId()).isNull();
        assertThat(semFinal.terceiroId()).isNull();

        // semis: 1 vence 4 e 2 vence 3. Final: 1 (A) x 2 (B); 3º lugar: 3 (A) x 4 (B)
        List<ResultadoMataMata> comFinal = new ArrayList<>(ate);
        comFinal.add(venceB(4, 1)); // seed 2 vence a final
        EstadoMataMata soFinal = formato(comTerceiro, comFinal);
        assertThat(soFinal.campeaoId()).isEqualTo(idDaSeed(2));
        assertThat(soFinal.viceId()).isEqualTo(idDaSeed(1));
        assertThat(soFinal.terceiroId()).isNull();

        List<ResultadoMataMata> completo = new ArrayList<>(comFinal);
        completo.add(venceB(3, 1)); // seed 4 vence a disputa de 3º
        EstadoMataMata podio = formato(comTerceiro, completo);
        assertThat(podio.campeaoId()).isEqualTo(idDaSeed(2));
        assertThat(podio.viceId()).isEqualTo(idDaSeed(1));
        assertThat(podio.terceiroId()).isEqualTo(idDaSeed(4));
    }

    @Test
    void podioDeDoisParticipantes() {
        EstadoMataMata estado = calcular(semTerceiro, 2, venceB(1, 1));

        assertThat(estado.campeaoId()).isEqualTo(idDaSeed(2));
        assertThat(estado.viceId()).isEqualTo(idDaSeed(1));
        assertThat(estado.terceiroId()).isNull();
    }

    @Test
    void campeaoDeUmaChaveComByeNaFinalVemDoBye() {
        // n = 3: 1 tem bye; 2x3 -> 2 vence; final 1x2 -> 1 vence
        EstadoMataMata estado = calcular(semTerceiro, 3, venceA(1, 2), venceA(2, 1));

        assertThat(estado.campeaoId()).isEqualTo(idDaSeed(1));
        assertThat(estado.viceId()).isEqualTo(idDaSeed(2));
    }

    private static EstadoMataMata formato(ConfigMataMata config, List<ResultadoMataMata> resultados) {
        return MataMataFixtures.FORMATO.calcular(config, participantes(8), resultados, SEED);
    }
}
