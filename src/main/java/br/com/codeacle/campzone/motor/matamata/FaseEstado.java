package br.com.codeacle.campzone.motor.matamata;

import java.util.List;

/**
 * Uma fase da chave.
 *
 * @param ordem      ordem da fase na chave, a partir de 1; com a disputa de 3º, ela ocupa a ordem da final e a final
 *                   passa a ter a ordem seguinte
 * @param nome       nome para exibir ("Oitavas de final", "Semifinal", "Final"...)
 * @param tipo       papel da fase
 * @param confrontos confrontos da fase, por posição
 */
public record FaseEstado(int ordem, String nome, TipoFase tipo, List<ConfrontoEstado> confrontos) {

    /** Copia a lista de confrontos. */
    public FaseEstado {
        confrontos = List.copyOf(confrontos);
    }
}
