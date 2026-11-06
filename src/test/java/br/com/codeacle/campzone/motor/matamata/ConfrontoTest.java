package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.calcular;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.confronto;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idDaSeed;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idaEVolta;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogoUnico;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.placar;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.serie;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.situacoes;
import static br.com.codeacle.campzone.motor.matamata.SituacaoJogo.DESNECESSARIO;
import static br.com.codeacle.campzone.motor.matamata.SituacaoJogo.DISPUTADO;
import static br.com.codeacle.campzone.motor.matamata.SituacaoJogo.PENDENTE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Confrontos em jogo único, ida e volta e série, vistos pela final de uma chave de 2 participantes (fase 1, confronto 1).
 * O A é a seed 1 (id 10) e o B é a seed 2 (id 17).
 */
class ConfrontoTest {

    private static final long A = idDaSeed(1);
    private static final long B = idDaSeed(2);

    /** O confronto único de uma chave de 2 participantes. */
    private ConfrontoEstado decisao(ConfigMataMata config, ResultadoMataMata... resultados) {
        return confronto(calcular(config, 2, resultados), 1, 1);
    }

    private static JogoEstado jogo(long mandante, long visitante, SituacaoJogo situacao, int numero) {
        return new JogoEstado(numero, mandante, visitante, situacao);
    }

    // ---------------------------------------------------------------- jogo único

    @Test
    void jogoUnicoTemUmJogoComAMelhorSeedEmCasa() {
        ConfrontoEstado confronto = decisao(jogoUnico(PENALTIS));

        assertThat(confronto.participanteAId()).isEqualTo(A);
        assertThat(confronto.participanteBId()).isEqualTo(B);
        assertThat(confronto.bye()).isFalse();
        assertThat(confronto.vencedorId()).isNull();
        assertThat(confronto.jogos()).containsExactly(jogo(A, B, PENDENTE, 1));
    }

    @Test
    void jogoUnicoOPlacarDecideParaOMandante() {
        ConfrontoEstado confronto = decisao(jogoUnico(PENALTIS), placar(1, 1, 1, 2, 1));

        assertThat(confronto.vencedorId()).isEqualTo(A);
        assertThat(confronto.jogos()).containsExactly(jogo(A, B, DISPUTADO, 1));
    }

    @Test
    void jogoUnicoOPlacarDecideParaOVisitante() {
        ConfrontoEstado confronto = decisao(jogoUnico(PENALTIS), placar(1, 1, 1, 0, 3));

        assertThat(confronto.vencedorId()).isEqualTo(B);
    }

    // ---------------------------------------------------------------- ida e volta

    @Test
    void idaEVoltaTemOMandoDaIdaComOBEOdaVoltaComOA() {
        ConfrontoEstado confronto = decisao(idaEVolta(PENALTIS));

        assertThat(confronto.jogos()).containsExactly(jogo(B, A, PENDENTE, 1), jogo(A, B, PENDENTE, 2));
        assertThat(confronto.vencedorId()).isNull();
    }

    @Test
    void idaEVoltaSoComAIdaFicaSemVencedorEComAVoltaPendente() {
        ConfrontoEstado confronto = decisao(idaEVolta(PENALTIS), placar(1, 1, 1, 5, 0));

        assertThat(confronto.vencedorId()).isNull();
        assertThat(situacoes(confronto)).containsExactly(DISPUTADO, PENDENTE);
    }

    @Test
    void idaEVoltaDecidePeloAgregadoEPodeVirarOJogo() {
        // ida: B 3 x 1 A; volta: A 3 x 0 B -> A 4, B 3: A vence mesmo perdendo a ida
        ConfrontoEstado confronto = decisao(idaEVolta(PENALTIS), placar(1, 1, 1, 3, 1), placar(1, 1, 2, 3, 0));

        assertThat(confronto.vencedorId()).isEqualTo(A);
        assertThat(situacoes(confronto)).containsExactly(DISPUTADO, DISPUTADO);
    }

    @Test
    void idaEVoltaOAgregadoDaVitoriaAoB() {
        // ida: B 2 x 0 A; volta: A 1 x 1 B -> A 1, B 3
        ConfrontoEstado confronto = decisao(idaEVolta(PENALTIS), placar(1, 1, 1, 2, 0), placar(1, 1, 2, 1, 1));

        assertThat(confronto.vencedorId()).isEqualTo(B);
    }

    // ---------------------------------------------------------------- série

    @Test
    void serieAlternaOMandoAPartirDoA() {
        ConfrontoEstado confronto = decisao(serie(5, PENALTIS));

        assertThat(confronto.jogos()).containsExactly(
                jogo(A, B, PENDENTE, 1),
                jogo(B, A, PENDENTE, 2),
                jogo(A, B, PENDENTE, 3),
                jogo(B, A, PENDENTE, 4),
                jogo(A, B, PENDENTE, 5));
    }

    @Test
    void serieMelhorDeTresVenceComDuasVitoriasEOJogoQueSobraFicaDesnecessario() {
        // jogo 1: A 1x0; jogo 2 (B em casa): A vence fora, 0x1
        ConfrontoEstado confronto = decisao(serie(3, PENALTIS), placar(1, 1, 1, 1, 0), placar(1, 1, 2, 0, 1));

        assertThat(confronto.vencedorId()).isEqualTo(A);
        assertThat(situacoes(confronto)).containsExactly(DISPUTADO, DISPUTADO, DESNECESSARIO);
    }

    @Test
    void serieMelhorDeTresVaiAoTerceiroJogoComUmaVitoriaParaCada() {
        ConfrontoEstado depoisDeDois = decisao(serie(3, PENALTIS), placar(1, 1, 1, 1, 0), placar(1, 1, 2, 1, 0));
        ConfrontoEstado depoisDeTres = decisao(serie(3, PENALTIS), placar(1, 1, 1, 1, 0), placar(1, 1, 2, 1, 0),
                placar(1, 1, 3, 0, 2));

        assertThat(depoisDeDois.vencedorId()).isNull();
        assertThat(situacoes(depoisDeDois)).containsExactly(DISPUTADO, DISPUTADO, PENDENTE);
        assertThat(depoisDeTres.vencedorId()).isEqualTo(B);
        assertThat(situacoes(depoisDeTres)).containsExactly(DISPUTADO, DISPUTADO, DISPUTADO);
    }

    @Test
    void serieMelhorDeCincoComVarridaDeixaDoisJogosDesnecessarios() {
        // B vence os 3 primeiros: jogo 1 (A em casa) 0x1, jogo 2 (B em casa) 1x0, jogo 3 (A em casa) 0x1
        ConfrontoEstado confronto = decisao(serie(5, PENALTIS), placar(1, 1, 1, 0, 1), placar(1, 1, 2, 1, 0),
                placar(1, 1, 3, 0, 1));

        assertThat(confronto.vencedorId()).isEqualTo(B);
        assertThat(situacoes(confronto)).containsExactly(DISPUTADO, DISPUTADO, DISPUTADO, DESNECESSARIO, DESNECESSARIO);
    }

    @Test
    void serieMelhorDeCincoSoEstaDecididaNaTerceiraVitoria() {
        ConfrontoEstado confronto = decisao(serie(5, PENALTIS), placar(1, 1, 1, 1, 0), placar(1, 1, 2, 0, 1),
                placar(1, 1, 3, 0, 1), placar(1, 1, 4, 1, 0));

        // A: jogos 1 e 2; B: jogo 3 e 4 -> 2 x 2, o quinto decide
        assertThat(confronto.vencedorId()).isNull();
        assertThat(situacoes(confronto)).containsExactly(DISPUTADO, DISPUTADO, DISPUTADO, DISPUTADO, PENDENTE);

        ConfrontoEstado decidido = decisao(serie(5, PENALTIS), placar(1, 1, 1, 1, 0), placar(1, 1, 2, 0, 1),
                placar(1, 1, 3, 0, 1), placar(1, 1, 4, 1, 0), placar(1, 1, 5, 2, 0));
        assertThat(decidido.vencedorId()).isEqualTo(A);
        assertThat(situacoes(decidido)).containsOnly(DISPUTADO);
    }

    @Test
    void serieSemResultadosDeixaTodosOsJogosPendentes() {
        ConfrontoEstado confronto = decisao(serie(7, PENALTIS));

        assertThat(confronto.jogos()).hasSize(7);
        assertThat(situacoes(confronto)).containsOnly(PENDENTE);
        assertThat(confronto.jogos()).extracting(JogoEstado::mandanteId)
                .containsExactly(A, B, A, B, A, B, A);
    }
}
