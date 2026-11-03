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
     *                                 {@code IDA_E_VOLTA}; se {@code SORTEIO} não for o último; ou se a lista não tiver
     *                                 um critério terminal ({@code PENALTIS} ou {@code SORTEIO}), sem o qual um empate
     *                                 seria impossível de lançar
     */
    public ConfigMataMata {
        if (tipoConfronto == null) {
            throw new ConfigInvalidaException("O tipo de confronto é obrigatório");
        }
        if (seeding == null) {
            throw new ConfigInvalidaException("O seeding é obrigatório");
        }
        if (tipoConfronto != TipoConfronto.SERIE && melhorDe != null) {
            throw new ConfigInvalidaException("melhorDe só pode ser informado numa série (SERIE), não em " + tipoConfronto);
        }
        if (tipoConfronto == TipoConfronto.SERIE && (melhorDe == null || melhorDe < 3 || melhorDe % 2 == 0)) {
            throw new ConfigInvalidaException("Numa série, melhorDe deve ser ímpar e pelo menos 3 (recebido: " + melhorDe + ")");
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
        int sorteio = desempates.indexOf(DesempateMataMata.SORTEIO);
        if (sorteio >= 0 && sorteio != desempates.size() - 1) {
            throw new ConfigInvalidaException("SORTEIO só pode ser o último critério de desempate");
        }
        if (!desempates.contains(DesempateMataMata.PENALTIS) && sorteio < 0) {
            throw new ConfigInvalidaException(
                    "Os desempates precisam de um critério final (PENALTIS ou SORTEIO); sem ele um empate não teria como ser decidido");
        }
        desempates = List.copyOf(desempates);
    }
}
