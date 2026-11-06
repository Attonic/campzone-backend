package br.com.codeacle.campzone.motor.pontoscorridos;

/**
 * Critérios de desempate da classificação, aplicados na ordem configurada. Enum próprio do motor: a camada de
 * aplicação traduz o enum do domínio JPA para este (ADR 0005).
 */
public enum CriterioDesempate {

    /** Pontos conquistados (maior primeiro). Sempre o primeiro critério. */
    PONTOS,
    /** Número de vitórias (maior primeiro). */
    VITORIAS,
    /** Saldo de gols/pontos (maior primeiro). */
    SALDO,
    /** Gols/pontos marcados (maior primeiro). */
    PRO,
    /** Pontos obtidos só nos jogos entre os empatados (mini-tabela). */
    CONFRONTO_DIRETO,
    /** Ordem do sorteio com a seed do campeonato. Quando usado, é o último critério. */
    SORTEIO
}
