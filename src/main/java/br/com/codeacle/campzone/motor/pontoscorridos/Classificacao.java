package br.com.codeacle.campzone.motor.pontoscorridos;

import java.util.List;

/** Classificação do pontos corridos: uma linha por participante, da primeira à última posição. */
public record Classificacao(List<LinhaClassificacao> linhas) {

    public Classificacao {
        linhas = List.copyOf(linhas);
    }
}
