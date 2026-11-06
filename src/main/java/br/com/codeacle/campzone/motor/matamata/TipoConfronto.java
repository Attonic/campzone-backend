package br.com.codeacle.campzone.motor.matamata;

/** Como dois participantes se enfrentam em cada confronto da chave. */
public enum TipoConfronto {

    /** Um único jogo, com a melhor seed (A) como mandante. */
    JOGO_UNICO,

    /** Dois jogos: a ida com a pior seed (B) como mandante e a volta com a melhor seed (A). Decide o agregado. */
    IDA_E_VOLTA,

    /**
     * Série melhor de N jogos ({@code melhorDe}), com o mando alternando a partir de A. Vence quem chegar à maioria.
     */
    SERIE
}
