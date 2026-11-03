package br.com.codeacle.campzone.motor.pontoscorridos;

import java.util.List;

/** Tabela completa de jogos do pontos corridos, na ordem das rodadas (numeradas de 1 até o total). */
public record TabelaPontosCorridos(List<RodadaGerada> rodadas) {

    public TabelaPontosCorridos {
        rodadas = List.copyOf(rodadas);
    }
}
