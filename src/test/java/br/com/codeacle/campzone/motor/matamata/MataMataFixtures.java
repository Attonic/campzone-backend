package br.com.codeacle.campzone.motor.matamata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.codeacle.campzone.motor.ParticipanteMotor;

/**
 * Apoio dos testes do mata-mata. Os participantes têm seeds de 1 a n no seeding MANUAL e ids 10, 17, 24...: a seed
 * {@code s} é o id {@code 10 + 7 * (s - 1)}, o que deixa as chaves fáceis de ler.
 */
final class MataMataFixtures {

    static final MataMataFormato FORMATO = new MataMataFormato();
    static final long SEED = 20260;

    private MataMataFixtures() {
    }

    // ---------------------------------------------------------------- configs

    static ConfigMataMata jogoUnico(DesempateMataMata... desempates) {
        return new ConfigMataMata(TipoConfronto.JOGO_UNICO, null, List.of(desempates), Seeding.MANUAL, false);
    }

    static ConfigMataMata idaEVolta(DesempateMataMata... desempates) {
        return new ConfigMataMata(TipoConfronto.IDA_E_VOLTA, null, List.of(desempates), Seeding.MANUAL, false);
    }

    static ConfigMataMata serie(int melhorDe, DesempateMataMata... desempates) {
        return new ConfigMataMata(TipoConfronto.SERIE, melhorDe, List.of(desempates), Seeding.MANUAL, false);
    }

    static ConfigMataMata comTerceiro(ConfigMataMata base, boolean disputaTerceiroLugar) {
        return new ConfigMataMata(base.tipoConfronto(), base.melhorDe(), base.desempates(), base.seeding(),
                disputaTerceiroLugar);
    }

    static ConfigMataMata comSeeding(ConfigMataMata base, Seeding seeding) {
        return new ConfigMataMata(base.tipoConfronto(), base.melhorDe(), base.desempates(), seeding,
                base.disputaTerceiroLugar());
    }

    // ---------------------------------------------------------------- participantes

    /** {@code n} participantes com seeds 1..n (a seed {@code s} tem o id {@link #idDaSeed(int)}). */
    static List<ParticipanteMotor> participantes(int n) {
        List<ParticipanteMotor> lista = new ArrayList<>();
        for (int seed = 1; seed <= n; seed++) {
            lista.add(new ParticipanteMotor(idDaSeed(seed), "Time " + seed, seed));
        }
        return lista;
    }

    static long idDaSeed(int seed) {
        return 10L + 7L * (seed - 1);
    }

    /** A seed de cada participante, pelo id. */
    static Map<Long, Integer> seeds(List<ParticipanteMotor> participantes) {
        Map<Long, Integer> seeds = new HashMap<>();
        participantes.forEach(p -> seeds.put(p.id(), p.seed()));
        return seeds;
    }

    // ---------------------------------------------------------------- resultados

    static ResultadoMataMata placar(int ordemFase, int posicao, int numeroJogo, int mandante, int visitante) {
        return new ResultadoMataMata(ordemFase, posicao, numeroJogo, mandante, visitante, null, null, null, null);
    }

    static ResultadoMataMata comProrrogacao(int ordemFase, int posicao, int numeroJogo, int mandante, int visitante,
            int prorrogacaoMandante, int prorrogacaoVisitante) {
        return new ResultadoMataMata(ordemFase, posicao, numeroJogo, mandante, visitante, prorrogacaoMandante,
                prorrogacaoVisitante, null, null);
    }

    static ResultadoMataMata comPenaltis(int ordemFase, int posicao, int numeroJogo, int mandante, int visitante,
            int penaltisMandante, int penaltisVisitante) {
        return new ResultadoMataMata(ordemFase, posicao, numeroJogo, mandante, visitante, null, null, penaltisMandante,
                penaltisVisitante);
    }

    static ResultadoMataMata completo(int ordemFase, int posicao, int numeroJogo, int mandante, int visitante,
            int prorrogacaoMandante, int prorrogacaoVisitante, int penaltisMandante, int penaltisVisitante) {
        return new ResultadoMataMata(ordemFase, posicao, numeroJogo, mandante, visitante, prorrogacaoMandante,
                prorrogacaoVisitante, penaltisMandante, penaltisVisitante);
    }

    // ---------------------------------------------------------------- leitura do estado

    static EstadoMataMata calcular(ConfigMataMata config, int n, ResultadoMataMata... resultados) {
        return FORMATO.calcular(config, participantes(n), List.of(resultados), SEED);
    }

    static FaseEstado fase(EstadoMataMata estado, int ordem) {
        return estado.fases().stream().filter(f -> f.ordem() == ordem).findFirst()
                .orElseThrow(() -> new AssertionError("Sem a fase de ordem " + ordem + " em " + descrever(estado)));
    }

    static ConfrontoEstado confronto(EstadoMataMata estado, int ordemFase, int posicao) {
        return fase(estado, ordemFase).confrontos().get(posicao - 1);
    }

    static List<SituacaoJogo> situacoes(ConfrontoEstado confronto) {
        return confronto.jogos().stream().map(JogoEstado::situacao).toList();
    }

    /** Os ids que ocupam as vagas (A e B) de todos os confrontos da fase, sem nulos, na ordem. */
    static List<Long> ocupantes(FaseEstado fase) {
        List<Long> ids = new ArrayList<>();
        for (ConfrontoEstado confronto : fase.confrontos()) {
            if (confronto.participanteAId() != null) {
                ids.add(confronto.participanteAId());
            }
            if (confronto.participanteBId() != null) {
                ids.add(confronto.participanteBId());
            }
        }
        return ids;
    }

    // ---------------------------------------------------------------- um torneio inteiro

    /** Decide como o jogo pendente termina; devolve o resultado a lançar. */
    @FunctionalInterface
    interface Politica {
        ResultadoMataMata jogar(FaseEstado fase, ConfrontoEstado confronto, JogoEstado jogo, Map<Long, Integer> seeds);
    }

    /** Quem tem a melhor seed vence cada jogo por 1 x 0. */
    static final Politica MELHOR_SEED_VENCE = (fase, confronto, jogo, seeds) -> vence(fase, confronto, jogo,
            seeds.get(jogo.mandanteId()) < seeds.get(jogo.visitanteId()));

    /** Quem tem a pior seed vence cada jogo por 1 x 0 (só zebras). */
    static final Politica PIOR_SEED_VENCE = (fase, confronto, jogo, seeds) -> vence(fase, confronto, jogo,
            seeds.get(jogo.mandanteId()) > seeds.get(jogo.visitanteId()));

    /** Vence o mandante, 1 x 0, ou o visitante, 0 x 1. */
    static ResultadoMataMata vence(FaseEstado fase, ConfrontoEstado confronto, JogoEstado jogo, boolean mandante) {
        return placar(fase.ordem(), confronto.posicao(), jogo.numeroJogo(), mandante ? 1 : 0, mandante ? 0 : 1);
    }

    /** O resultado final de {@link #jogarAteOFim}. */
    record Torneio(EstadoMataMata estado, List<ResultadoMataMata> resultados) {
    }

    /**
     * Joga sempre o primeiro jogo pendente (por fase, confronto e número do jogo) com a política dada, recalculando o
     * estado a cada resultado, até não haver jogo pendente.
     */
    static Torneio jogarAteOFim(ConfigMataMata config, List<ParticipanteMotor> participantes, long seedSorteio,
            Politica politica) {
        Map<Long, Integer> seeds = seeds(participantes);
        List<ResultadoMataMata> resultados = new ArrayList<>();
        EstadoMataMata estado = FORMATO.calcular(config, participantes, resultados, seedSorteio);
        for (int guarda = 0; guarda < 10_000; guarda++) {
            ResultadoMataMata proximo = primeiroPendente(estado, politica, seeds);
            if (proximo == null) {
                return new Torneio(estado, List.copyOf(resultados));
            }
            resultados.add(proximo);
            estado = FORMATO.calcular(config, participantes, resultados, seedSorteio);
        }
        throw new AssertionError("O torneio não terminou: " + descrever(estado));
    }

    private static ResultadoMataMata primeiroPendente(EstadoMataMata estado, Politica politica, Map<Long, Integer> seeds) {
        for (FaseEstado fase : estado.fases()) {
            for (ConfrontoEstado confronto : fase.confrontos()) {
                for (JogoEstado jogo : confronto.jogos()) {
                    if (jogo.situacao() == SituacaoJogo.PENDENTE) {
                        return politica.jogar(fase, confronto, jogo, seeds);
                    }
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- texto

    /** O estado em linhas legíveis, usado nas mensagens de falha e nos testes de golden. */
    static String descrever(EstadoMataMata estado) {
        return String.join("\n", linhas(estado));
    }

    static List<String> linhas(EstadoMataMata estado) {
        List<String> linhas = new ArrayList<>();
        for (FaseEstado fase : estado.fases()) {
            linhas.add("F" + fase.ordem() + " " + fase.nome() + " " + fase.tipo());
            for (ConfrontoEstado c : fase.confrontos()) {
                String jogos = c.jogos().stream().map(j -> "J" + j.numeroJogo() + ":" + id(j.mandanteId()) + "x"
                        + id(j.visitanteId()) + ":" + j.situacao()).collect(Collectors.joining(" "));
                linhas.add("  C" + c.posicao() + " " + id(c.participanteAId()) + "-" + id(c.participanteBId())
                        + (c.bye() ? " BYE" : "") + " vence=" + id(c.vencedorId()) + (jogos.isEmpty() ? "" : " | " + jogos));
            }
        }
        linhas.add("campeao=" + id(estado.campeaoId()) + " vice=" + id(estado.viceId()) + " terceiro="
                + id(estado.terceiroId()));
        return linhas;
    }

    private static String id(Long id) {
        return id == null ? "-" : String.valueOf(id);
    }
}
