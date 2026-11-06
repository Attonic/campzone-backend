package br.com.codeacle.campzone.motor.matamata;

/** Como os participantes recebem as seeds (posições de 1 a n) que montam a chave. */
public enum Seeding {

    /** Ignora a seed informada: as seeds saem do sorteio com a seed do campeonato. */
    ALEATORIO,

    /** Todos os participantes trazem uma seed única, de 1 a n. */
    MANUAL,

    /** Só os cabeças de chave trazem seed (1..k, sem buracos); os demais recebem k+1..n por sorteio. */
    CABECAS_DE_CHAVE
}
