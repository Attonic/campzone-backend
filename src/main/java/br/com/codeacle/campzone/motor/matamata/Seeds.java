package br.com.codeacle.campzone.motor.matamata;

import java.util.List;

import br.com.codeacle.campzone.motor.OrdemSorteada;
import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesInvalidosException;

/** Atribui as seeds de 1 a n aos participantes, conforme o {@link Seeding} da config. */
final class Seeds {

    private Seeds() {
    }

    /**
     * Ids dos participantes por seed: o índice 0 é a seed 1.
     *
     * <ul>
     *   <li>{@code ALEATORIO}: ignora a seed informada; a ordem sorteada dá as seeds a partir da 1;</li>
     *   <li>{@code MANUAL}: todos trazem uma seed única de 1 a n;</li>
     *   <li>{@code CABECAS_DE_CHAVE}: quem traz seed forma o conjunto 1..k sem buracos; os demais recebem k+1..n
     *       na ordem sorteada só entre eles.</li>
     * </ul>
     *
     * @param participantes já validados (pelo menos 2, ids únicos)
     * @throws ParticipantesInvalidosException se as seeds informadas não servirem ao seeding escolhido
     */
    static List<Long> porSeed(Seeding seeding, List<ParticipanteMotor> participantes, long seedSorteio) {
        return switch (seeding) {
            case ALEATORIO -> OrdemSorteada.de(participantes, seedSorteio);
            case MANUAL -> manual(participantes);
            case CABECAS_DE_CHAVE -> cabecasDeChave(participantes, seedSorteio);
        };
    }

    private static List<Long> manual(List<ParticipanteMotor> participantes) {
        int n = participantes.size();
        Long[] porSeed = new Long[n];
        for (ParticipanteMotor participante : participantes) {
            Integer seed = participante.seed();
            if (seed == null) {
                throw new ParticipantesInvalidosException("No seeding MANUAL todos os participantes precisam de seed: "
                        + "falta ao participante " + participante.id());
            }
            ocupar(porSeed, participante, seed, n, "no seeding MANUAL as seeds vão de 1 a " + n);
        }
        return List.of(porSeed);
    }

    private static List<Long> cabecasDeChave(List<ParticipanteMotor> participantes, long seedSorteio) {
        List<ParticipanteMotor> cabecas = participantes.stream().filter(p -> p.seed() != null).toList();
        List<ParticipanteMotor> demais = participantes.stream().filter(p -> p.seed() == null).toList();
        int k = cabecas.size();
        Long[] porSeed = new Long[participantes.size()];
        for (ParticipanteMotor cabeca : cabecas) {
            ocupar(porSeed, cabeca, cabeca.seed(), k,
                    "com " + k + " cabeças de chave as seeds informadas devem ser 1.." + k + ", sem buracos");
        }
        List<Long> sorteados = OrdemSorteada.de(demais, seedSorteio);
        for (int i = 0; i < sorteados.size(); i++) {
            porSeed[k + i] = sorteados.get(i);
        }
        return List.of(porSeed);
    }

    /** Coloca o participante na seed informada, que deve estar em 1..limite e livre. */
    private static void ocupar(Long[] porSeed, ParticipanteMotor participante, int seed, int limite, String regra) {
        if (seed < 1 || seed > limite) {
            throw new ParticipantesInvalidosException("A seed " + seed + " do participante " + participante.id()
                    + " é inválida: " + regra);
        }
        Long ocupante = porSeed[seed - 1];
        if (ocupante != null) {
            throw new ParticipantesInvalidosException("Seed repetida: " + seed + " (participantes " + ocupante + " e "
                    + participante.id() + ")");
        }
        porSeed[seed - 1] = participante.id();
    }
}
