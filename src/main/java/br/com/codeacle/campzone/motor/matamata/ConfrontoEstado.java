package br.com.codeacle.campzone.motor.matamata;

import java.util.List;

/**
 * Um confronto da chave.
 *
 * @param posicao         posição do confronto na fase, a partir de 1
 * @param participanteAId a melhor seed do confronto, ou {@code null} enquanto a vaga não é conhecida
 * @param participanteBId a pior seed, ou {@code null} enquanto a vaga não é conhecida (e sempre nulo num bye)
 * @param bye             se a melhor seed avança sem jogar (não há jogos)
 * @param vencedorId      quem venceu o confronto, ou {@code null} se ainda não foi decidido; num bye já é o A
 * @param jogos           jogos do confronto, na ordem; vazia num bye
 */
public record ConfrontoEstado(int posicao, Long participanteAId, Long participanteBId, boolean bye, Long vencedorId,
        List<JogoEstado> jogos) {

    /** Copia a lista de jogos. */
    public ConfrontoEstado {
        jogos = List.copyOf(jogos);
    }
}
