package br.com.codeacle.campzone.motor.matamata;

import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.GOLS_FORA;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PENALTIS;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.PRORROGACAO;
import static br.com.codeacle.campzone.motor.matamata.DesempateMataMata.SORTEIO;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comPenaltis;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.comProrrogacao;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.idaEVolta;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.jogoUnico;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.placar;
import static br.com.codeacle.campzone.motor.matamata.MataMataFixtures.serie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.motor.ResultadoInvalidoException;

/** O avaliador de confronto e as regras de desempate, direto, sem montar uma chave. */
class AvaliadorConfrontoTest {

    private static final long A = 10;
    private static final long B = 17;

    /** Disputa em que A é o mandante do jogo decisivo, sem gols fora (jogo único ou jogo de série). */
    private static AvaliadorConfronto.Disputa disputa(ResultadoMataMata decisivo, Integer golsForaA, Integer golsForaB,
            boolean sorteioFavoreceA) {
        return new AvaliadorConfronto.Disputa("jogo X", A, B, decisivo.placarMandante(), decisivo.placarVisitante(),
                golsForaA, golsForaB, decisivo, true, sorteioFavoreceA);
    }

    // ---------------------------------------------------------------- quantidade e mando

    @Test
    void quantidadeDeJogosPorTipoDeConfronto() {
        assertThat(AvaliadorConfronto.quantidadeDeJogos(jogoUnico(PENALTIS))).isEqualTo(1);
        assertThat(AvaliadorConfronto.quantidadeDeJogos(idaEVolta(PENALTIS))).isEqualTo(2);
        assertThat(AvaliadorConfronto.quantidadeDeJogos(serie(3, PENALTIS))).isEqualTo(3);
        assertThat(AvaliadorConfronto.quantidadeDeJogos(serie(7, PENALTIS))).isEqualTo(7);
    }

    @Test
    void mandoDeCadaTipoDeConfronto() {
        ConfigMataMata unico = jogoUnico(PENALTIS);
        ConfigMataMata idaEVolta = idaEVolta(PENALTIS);
        ConfigMataMata serie = serie(5, PENALTIS);

        assertThat(AvaliadorConfronto.mandanteEhA(unico, 1)).isTrue();
        assertThat(AvaliadorConfronto.mandanteEhA(idaEVolta, 1)).isFalse();
        assertThat(AvaliadorConfronto.mandanteEhA(idaEVolta, 2)).isTrue();
        assertThat(List.of(1, 2, 3, 4, 5)).extracting(n -> AvaliadorConfronto.mandanteEhA(serie, n))
                .containsExactly(true, false, true, false, true);
    }

    // ---------------------------------------------------------------- decidir

    @Test
    void oPlacarDecideSemConsultarOsCriterios() {
        assertThat(AvaliadorConfronto.decidir(List.of(PENALTIS), disputa(placar(1, 1, 1, 3, 2), null, null, true)))
                .isEqualTo(A);
        assertThat(AvaliadorConfronto.decidir(List.of(PENALTIS), disputa(placar(1, 1, 1, 0, 1), null, null, true)))
                .isEqualTo(B);
    }

    @Test
    void golsForaSemGolsForaNaDisputaEPulado() {
        // jogo único ou jogo de série: não há gols fora, e o critério é pulado (a config já impede essa combinação)
        AvaliadorConfronto.Disputa empatada = disputa(comPenaltis(1, 1, 1, 1, 1, 3, 4), null, null, true);

        assertThat(AvaliadorConfronto.decidir(List.of(GOLS_FORA, PENALTIS), empatada)).isEqualTo(B);
    }

    @Test
    void golsForaEmpatadoPassaAoCriterioSeguinte() {
        AvaliadorConfronto.Disputa empatada = disputa(comPenaltis(1, 1, 2, 1, 1, 5, 3), 2, 2, false);

        assertThat(AvaliadorConfronto.decidir(List.of(GOLS_FORA, PENALTIS), empatada)).isEqualTo(A);
    }

    @Test
    void golsForaDecidemQuandoDiferem() {
        assertThat(AvaliadorConfronto.decidir(List.of(GOLS_FORA, PENALTIS),
                disputa(placar(1, 1, 2, 1, 1), 1, 2, false))).isEqualTo(B);
        assertThat(AvaliadorConfronto.decidir(List.of(GOLS_FORA, PENALTIS),
                disputa(placar(1, 1, 2, 1, 1), 3, 2, false))).isEqualTo(A);
    }

    @Test
    void sorteioDecideConformeOResultadoDoSorteio() {
        assertThat(AvaliadorConfronto.decidir(List.of(SORTEIO), disputa(placar(1, 1, 1, 0, 0), null, null, true)))
                .isEqualTo(A);
        assertThat(AvaliadorConfronto.decidir(List.of(SORTEIO), disputa(placar(1, 1, 1, 0, 0), null, null, false)))
                .isEqualTo(B);
    }

    @Test
    void recusaQuandoNenhumCriterioDecide() {
        // só acontece com uma lista sem critério terminal, que a ConfigMataMata não deixa criar
        assertThatThrownBy(() -> AvaliadorConfronto.decidir(List.of(PRORROGACAO),
                disputa(comProrrogacao(1, 1, 1, 1, 1, 0, 0), null, null, true)))
                .isInstanceOf(ResultadoInvalidoException.class).hasMessageContaining("nenhum critério");
    }

    @Test
    void recusaPenaltisEmpatados() {
        assertThatThrownBy(() -> AvaliadorConfronto.decidir(List.of(PENALTIS),
                disputa(comPenaltis(1, 1, 1, 1, 1, 4, 4), null, null, true)))
                .isInstanceOf(ResultadoInvalidoException.class).hasMessageContaining("empatados");
    }

    @Test
    void oVisitanteDoJogoDecisivoTemOsDadosInvertidos() {
        // série: B é o mandante do jogo 2; a prorrogação 0 x 1 é do visitante, que é o A
        ResultadoMataMata jogo2 = comProrrogacao(1, 1, 2, 1, 1, 0, 1);
        AvaliadorConfronto.Disputa disputa = new AvaliadorConfronto.Disputa("jogo 2", A, B, 1, 1, null, null, jogo2,
                false, true);

        assertThat(disputa.prorrogacaoA()).isEqualTo(1);
        assertThat(disputa.prorrogacaoB()).isZero();
        assertThat(AvaliadorConfronto.decidir(List.of(PRORROGACAO, SORTEIO), disputa)).isEqualTo(A);
    }
}
