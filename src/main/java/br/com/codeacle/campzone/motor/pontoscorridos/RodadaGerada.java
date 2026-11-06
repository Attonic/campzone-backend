package br.com.codeacle.campzone.motor.pontoscorridos;

import java.util.List;

/**
 * Uma rodada da tabela.
 *
 * @param numero  número da rodada, de 1 até o total, contínuo entre os turnos
 * @param turno   turno a que a rodada pertence (começa em 1)
 * @param jogos   jogos da rodada; ninguém aparece em dois jogos
 * @param folgaId quem folga na rodada (com número ímpar de participantes), ou {@code null} se ninguém folga
 */
public record RodadaGerada(int numero, int turno, List<JogoGerado> jogos, Long folgaId) {

    public RodadaGerada {
        jogos = List.copyOf(jogos);
    }

    /** A mesma rodada, com outro número e turno e o mando de todos os jogos invertido. */
    RodadaGerada comMandoInvertido(int novoNumero, int novoTurno) {
        return new RodadaGerada(novoNumero, novoTurno, jogos.stream().map(JogoGerado::invertido).toList(), folgaId);
    }

    /** A mesma rodada, com outro número e turno e o mesmo mando. */
    RodadaGerada repetida(int novoNumero, int novoTurno) {
        return new RodadaGerada(novoNumero, novoTurno, jogos, folgaId);
    }
}
