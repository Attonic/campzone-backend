package br.com.codeacle.campzone.motor.matamata;

/**
 * Critérios para decidir um confronto empatado, aplicados na ordem configurada e só quando necessários.
 * {@link #PENALTIS} e {@link #SORTEIO} sempre decidem; os demais podem empatar e passar ao critério seguinte.
 */
public enum DesempateMataMata {

    /** Gols marcados como visitante, no tempo normal. Só vale na ida e volta. */
    GOLS_FORA,

    /** Vence quem marcou mais na prorrogação (só os gols da prorrogação, não o acumulado). */
    PRORROGACAO,

    /** Vence quem fez mais pênaltis. Pênaltis empatados tornam o resultado inválido. */
    PENALTIS,

    /** Sorteio reprodutível por confronto, sem dados no resultado. Deve ser o último critério. */
    SORTEIO
}
