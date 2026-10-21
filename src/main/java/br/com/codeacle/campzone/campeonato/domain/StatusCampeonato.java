package br.com.codeacle.campzone.campeonato.domain;

/** Ciclo de vida do campeonato. Contam para o limite do plano os campeonatos RASCUNHO e EM_ANDAMENTO (ativos). */
public enum StatusCampeonato {
    RASCUNHO, EM_ANDAMENTO, FINALIZADO
}
