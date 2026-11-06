package br.com.codeacle.campzone.motor.matamata;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;

/**
 * Configuração do formato mata-mata (eliminatória simples). O mesmo {@code tipoConfronto} vale para todas as fases,
 * inclusive a final e a disputa de 3º lugar.
 *
 * @param tipoConfronto         jogo único, ida e volta ou série
 * @param melhorDe              tamanho da série (ímpar, pelo menos 3); só informado quando o tipo é {@code SERIE}
 * @param desempates            critérios de desempate, na ordem de aplicação
 * @param seeding               como os participantes recebem as seeds
 * @param disputaTerceiroLugar  se há disputa de 3º lugar entre os perdedores das semifinais
 */
public record ConfigMataMata(TipoConfronto tipoConfronto, Integer melhorDe, List<DesempateMataMata> desempates,
        Seeding seeding, boolean disputaTerceiroLugar) {

    /**
     * Valida a configuração e copia a lista de desempates.
     *
     * @throws ConfigInvalidaException se o tipo de confronto ou o seeding forem nulos; se {@code melhorDe} for
     *                                 informado fora de {@code SERIE}, ou numa série não for ímpar e pelo menos 3; se
     *                                 os desempates forem vazios ou repetidos; se {@code GOLS_FORA} aparecer sem
     *                                 {@code IDA_E_VOLTA}; se {@code SORTEIO} aparecer numa {@code SERIE}; ou se a
     *                                 lista não terminar no primeiro critério terminal ({@code PENALTIS} ou
     *                                 {@code SORTEIO}), seja por não ter nenhum (um empate seria impossível de
     *                                 lançar) ou por ter algum critério depois dele (que nunca seria usado)
     */
    public ConfigMataMata {
        if (tipoConfronto == null) {
            throw new ConfigInvalidaException("O tipo de confronto é obrigatório");
        }
        if (seeding == null) {
            throw new ConfigInvalidaException("O seeding é obrigatório");
        }
        if (tipoConfronto != TipoConfronto.SERIE && melhorDe != null) {
            throw new ConfigInvalidaException(
                    "melhorDe só pode ser informado numa série (SERIE), não em " + tipoConfronto);
        }
        if (tipoConfronto == TipoConfronto.SERIE && (melhorDe == null || melhorDe < 3 || melhorDe % 2 == 0)) {
            throw new ConfigInvalidaException(
                    "Numa série, melhorDe deve ser ímpar e pelo menos 3 (recebido: " + melhorDe + ")");
        }
        if (desempates == null || desempates.isEmpty() || desempates.stream().anyMatch(Objects::isNull)) {
            throw new ConfigInvalidaException("Informe ao menos um critério de desempate");
        }
        if (new HashSet<>(desempates).size() != desempates.size()) {
            throw new ConfigInvalidaException("Os critérios de desempate não podem se repetir");
        }
        if (desempates.contains(DesempateMataMata.GOLS_FORA) && tipoConfronto != TipoConfronto.IDA_E_VOLTA) {
            throw new ConfigInvalidaException("GOLS_FORA só vale na ida e volta");
        }
        if (tipoConfronto == TipoConfronto.SERIE && desempates.contains(DesempateMataMata.SORTEIO)) {
            throw new ConfigInvalidaException(
                    "SORTEIO não vale numa série: sortear o vencedor de um jogo de série não faz sentido em"
                            + " campeonato");
        }
        int terminal = indiceDoPrimeiroTerminal(desempates);
        if (terminal < 0) {
            throw new ConfigInvalidaException(
                    "Os desempates precisam terminar num critério final (PENALTIS ou SORTEIO); sem ele um empate"
                            + " não teria como ser decidido");
        }
        if (terminal != desempates.size() - 1) {
            throw new ConfigInvalidaException("Nenhum critério pode vir depois de " + desempates.get(terminal)
                    + ", que sempre decide: ele deve ser o último dos desempates");
        }
        desempates = List.copyOf(desempates);
    }

    /** Posição do primeiro critério que sempre decide ({@code PENALTIS} ou {@code SORTEIO}), ou -1 se não houver. */
    private static int indiceDoPrimeiroTerminal(List<DesempateMataMata> desempates) {
        for (int i = 0; i < desempates.size(); i++) {
            if (desempates.get(i) == DesempateMataMata.PENALTIS || desempates.get(i) == DesempateMataMata.SORTEIO) {
                return i;
            }
        }
        return -1;
    }
}
