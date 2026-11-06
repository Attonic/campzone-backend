package br.com.codeacle.campzone.motor.pontoscorridos;

/** Um jogo da tabela, identificado pelos ids dos participantes. */
public record JogoGerado(long mandanteId, long visitanteId) {

    /** O mesmo confronto com o mando invertido. */
    JogoGerado invertido() {
        return new JogoGerado(visitanteId, mandanteId);
    }
}
