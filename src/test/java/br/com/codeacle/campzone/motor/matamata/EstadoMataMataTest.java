package br.com.codeacle.campzone.motor.matamata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Os records do estado copiam suas listas: o estado devolvido pelo motor não muda depois de criado. */
class EstadoMataMataTest {

    @Test
    void jogosDoConfrontoSaoCopiadosEImutaveis() {
        List<JogoEstado> jogos = new ArrayList<>(List.of(new JogoEstado(1, 10L, 17L, SituacaoJogo.PENDENTE)));
        ConfrontoEstado confronto = new ConfrontoEstado(1, 10L, 17L, false, null, jogos);

        jogos.add(new JogoEstado(2, 17L, 10L, SituacaoJogo.PENDENTE));

        assertThat(confronto.jogos()).hasSize(1);
        assertThatThrownBy(() -> confronto.jogos().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void confrontosDaFaseSaoCopiadosEImutaveis() {
        List<ConfrontoEstado> confrontos = new ArrayList<>(List.of(new ConfrontoEstado(1, 10L, null, true, 10L, List.of())));
        FaseEstado fase = new FaseEstado(1, "Final", TipoFase.FINAL, confrontos);

        confrontos.clear();

        assertThat(fase.confrontos()).hasSize(1);
        assertThatThrownBy(() -> fase.confrontos().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fasesDoEstadoSaoCopiadasEImutaveis() {
        List<FaseEstado> fases = new ArrayList<>(List.of(new FaseEstado(1, "Final", TipoFase.FINAL, List.of())));
        EstadoMataMata estado = new EstadoMataMata(fases, null, null, null);

        fases.clear();

        assertThat(estado.fases()).hasSize(1);
        assertThatThrownBy(() -> estado.fases().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
