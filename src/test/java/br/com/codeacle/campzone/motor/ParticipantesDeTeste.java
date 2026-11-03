package br.com.codeacle.campzone.motor;

import java.util.ArrayList;
import java.util.List;

/** Fábrica de participantes para os testes do motor. Os ids não são contíguos de propósito. */
public final class ParticipantesDeTeste {

    private ParticipantesDeTeste() {
    }

    /** {@code n} participantes com ids 10, 17, 24... e nomes "Time 1", "Time 2"... */
    public static List<ParticipanteMotor> com(int n) {
        List<ParticipanteMotor> lista = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            lista.add(new ParticipanteMotor(10L + 7L * i, "Time " + (i + 1), null));
        }
        return lista;
    }

    /** Participantes com os ids informados. */
    public static List<ParticipanteMotor> comIds(long... ids) {
        List<ParticipanteMotor> lista = new ArrayList<>();
        for (long id : ids) {
            lista.add(new ParticipanteMotor(id, "Time " + id, null));
        }
        return lista;
    }
}
