package br.com.codeacle.campzone.motor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Ordem sorteada dos participantes, única para o campeonato: parte dos ids em ordem crescente (assim a entrada em
 * qualquer ordem dá o mesmo resultado) e embaralha com {@code new Random(seedSorteio)}, cujo algoritmo o JDK fixa em
 * contrato. No pontos corridos, a geração da tabela e o critério SORTEIO usam esta mesma ordem; no mata-mata, ela
 * dá as seeds do seeding aleatório e dos cabeças de chave.
 */
public final class OrdemSorteada {

    private OrdemSorteada() {
    }

    /** Ids dos participantes na ordem sorteada. */
    public static List<Long> de(List<ParticipanteMotor> participantes, long seedSorteio) {
        List<Long> ids = new ArrayList<>(participantes.stream().map(ParticipanteMotor::id).sorted().toList());
        Collections.shuffle(ids, new Random(seedSorteio));
        return ids;
    }
}
