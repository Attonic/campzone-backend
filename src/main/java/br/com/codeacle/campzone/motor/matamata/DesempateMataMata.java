package br.com.codeacle.campzone.motor.matamata;

/**
 * Critérios para decidir um confronto empatado, aplicados na ordem configurada e só quando necessários.
 * {@link #PENALTIS} e {@link #SORTEIO} sempre decidem (são os terminais); os demais podem empatar e passar ao critério
 * seguinte. A lista configurada termina no primeiro terminal, e nada pode vir depois dele.
 */
public enum DesempateMataMata {

    /** Gols marcados como visitante, no tempo normal. Só vale na ida e volta. */
    GOLS_FORA,

    /** Vence quem marcou mais na prorrogação (só os gols da prorrogação, não o acumulado). */
    PRORROGACAO,

    /** Vence quem fez mais pênaltis. Pênaltis empatados tornam o resultado inválido. */
    PENALTIS,

    /** Sorteio reprodutível por confronto, sem dados no resultado. Não vale na série. */
    SORTEIO
}
