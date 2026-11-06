package br.com.codeacle.campzone.motor.pontoscorridos;

/**
 * Linha da classificação. "Gols" é o nome genérico do placar (pontos, gols, partidas...).
 *
 * @param posicao        posição (1 = líder); empatados que sobrevivem a todos os critérios dividem a posição
 * @param participanteId id do participante
 * @param saldo          {@code golsPro - golsContra}
 */
public record LinhaClassificacao(int posicao, long participanteId, int pontos, int jogos, int vitorias, int empates,
        int derrotas, int golsPro, int golsContra, int saldo) {
}
