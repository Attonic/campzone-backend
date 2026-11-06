package br.com.codeacle.campzone.motor.matamata;

import java.util.List;

/**
 * Estado da chave, sempre recalculado a partir dos resultados (ADR 0005, Adendo A). É a estrutura gerada
 * ({@code gerar}) e o estado calculado ({@code calcular}) do mata-mata.
 *
 * @param fases      fases em ordem; a disputa de 3º, quando existe, vem antes da final
 * @param campeaoId  campeão, ou {@code null} até a final ser decidida
 * @param viceId     vice-campeão, ou {@code null} até a final ser decidida
 * @param terceiroId vencedor da disputa de 3º, ou {@code null} sem a disputa ou antes de ela ser decidida
 */
public record EstadoMataMata(List<FaseEstado> fases, Long campeaoId, Long viceId, Long terceiroId) {

    /** Copia a lista de fases. */
    public EstadoMataMata {
        fases = List.copyOf(fases);
    }
}
