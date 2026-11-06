package br.com.codeacle.campzone.motor.matamata;

/** Situação de um jogo da chave, calculada a partir dos resultados. */
public enum SituacaoJogo {

    /** O confronto ainda não tem os dois participantes (a fase anterior não fechou). */
    AGUARDANDO_PARTICIPANTES,

    /** O confronto tem os dois participantes e o jogo ainda não tem resultado. */
    PENDENTE,

    /** O jogo já tem resultado. */
    DISPUTADO,

    /** O confronto já foi decidido antes deste jogo (numa série, os jogos que sobram). Não aceita resultado. */
    DESNECESSARIO
}
