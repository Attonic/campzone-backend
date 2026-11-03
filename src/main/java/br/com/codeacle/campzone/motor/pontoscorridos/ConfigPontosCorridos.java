package br.com.codeacle.campzone.motor.pontoscorridos;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;

/**
 * Configuração do formato pontos corridos.
 *
 * @param turnos        quantas vezes todos se enfrentam (1 = turno único, 2 = ida e volta...)
 * @param pontosVitoria pontos por vitória
 * @param pontosEmpate  pontos por empate
 * @param pontosDerrota pontos por derrota (pode ser negativo)
 * @param permiteEmpate se {@code false}, um jogo empatado é rejeitado
 * @param criterios     critérios de desempate, na ordem de aplicação
 * @throws ConfigInvalidaException se {@code turnos < 1}; se os pontos não respeitarem
 *                                 {@code vitória > derrota} e {@code vitória >= empate >= derrota}; se os critérios
 *                                 forem vazios ou repetidos; se {@link CriterioDesempate#PONTOS} não for o primeiro;
 *                                 ou se {@link CriterioDesempate#SORTEIO} não for o último
 */
public record ConfigPontosCorridos(int turnos, int pontosVitoria, int pontosEmpate, int pontosDerrota,
        boolean permiteEmpate, List<CriterioDesempate> criterios) {

    public ConfigPontosCorridos {
        if (turnos < 1) {
            throw new ConfigInvalidaException("O número de turnos deve ser pelo menos 1");
        }
        if (pontosVitoria <= pontosDerrota || pontosVitoria < pontosEmpate || pontosEmpate < pontosDerrota) {
            throw new ConfigInvalidaException("Pontos incoerentes: é preciso vitória > derrota e vitória >= empate >= derrota");
        }
        if (criterios == null || criterios.isEmpty() || criterios.stream().anyMatch(Objects::isNull)) {
            throw new ConfigInvalidaException("Informe ao menos um critério de desempate");
        }
        if (new HashSet<>(criterios).size() != criterios.size()) {
            throw new ConfigInvalidaException("Os critérios de desempate não podem se repetir");
        }
        if (criterios.get(0) != CriterioDesempate.PONTOS) {
            throw new ConfigInvalidaException("PONTOS deve ser o primeiro critério de desempate");
        }
        int sorteio = criterios.indexOf(CriterioDesempate.SORTEIO);
        if (sorteio >= 0 && sorteio != criterios.size() - 1) {
            throw new ConfigInvalidaException("SORTEIO só pode ser o último critério de desempate");
        }
        criterios = List.copyOf(criterios);
    }

    /** Config padrão: 3/1/0, empate permitido e critérios PONTOS, VITORIAS, SALDO, PRO, CONFRONTO_DIRETO, SORTEIO. */
    public static ConfigPontosCorridos padrao(int turnos) {
        return new ConfigPontosCorridos(turnos, 3, 1, 0, true, List.of(CriterioDesempate.PONTOS,
                CriterioDesempate.VITORIAS, CriterioDesempate.SALDO, CriterioDesempate.PRO,
                CriterioDesempate.CONFRONTO_DIRETO, CriterioDesempate.SORTEIO));
    }
}
