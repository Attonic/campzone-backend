package br.com.codeacle.campzone.motor;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Regras comuns aos formatos para a lista de participantes: pelo menos 2, sem nulos e com ids únicos. */
public final class ValidadorParticipantes {

    /** Quantidade mínima de participantes para haver competição. */
    public static final int MINIMO = 2;

    private ValidadorParticipantes() {
    }

    /**
     * Valida a lista (o nome vazio já é barrado pelo próprio {@link ParticipanteMotor}).
     *
     * @throws ParticipantesInvalidosException se a lista for nula, tiver nulos, menos de {@value #MINIMO} itens ou ids repetidos
     */
    public static void validar(List<ParticipanteMotor> participantes) {
        if (participantes == null || participantes.stream().anyMatch(Objects::isNull)) {
            throw new ParticipantesInvalidosException("A lista de participantes é obrigatória e não aceita itens nulos");
        }
        if (participantes.size() < MINIMO) {
            throw new ParticipantesInvalidosException(
                    "São necessários pelo menos " + MINIMO + " participantes (recebidos: " + participantes.size() + ")");
        }
        Set<Long> vistos = new HashSet<>();
        for (ParticipanteMotor participante : participantes) {
            if (!vistos.add(participante.id())) {
                throw new ParticipantesInvalidosException("Participante repetido: id " + participante.id());
            }
        }
    }
}
