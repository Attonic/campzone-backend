package br.com.codeacle.campzone.motor.matamata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ResultadoInvalidoException;

/**
 * Monta o estado da chave como função pura dos participantes, da config, dos resultados e da seed do sorteio
 * (ADR 0005, Adendo A): nada é guardado ou "avançado", tudo é recalculado de cima para baixo, fase a fase.
 *
 * <p>A primeira fase nasce da ordem clássica das seeds. Cada fase seguinte só recebe participantes quando todos os
 * confrontos da anterior têm vencedor; antes disso seus jogos ficam aguardando. As semifinais, quando fecham, abrem
 * juntas a disputa de 3º (se configurada e se os dois perdedores forem reais) e a final.
 */
final class CalculadoraMataMata {

    /** Posição de um jogo na chave, a chave dos resultados. */
    private record ChaveJogo(int ordemFase, int posicao, int numeroJogo) {
    }

    /** Participantes de um confronto: A (a melhor seed) e B, que é {@code null} num bye. */
    private record Vagas(long a, Long b) {

        boolean bye() {
            return b == null;
        }
    }

    private final ConfigMataMata config;
    private final long seedSorteio;
    /** Ids por seed: o índice 0 é a seed 1. */
    private final List<Long> idPorSeed;
    private final Map<Long, Integer> seedPorId = new HashMap<>();
    /** Resultados ainda não encaixados em algum jogo; o que sobrar no fim aponta para um jogo que não existe. */
    private final Map<ChaveJogo, ResultadoMataMata> naoEncaixados = new LinkedHashMap<>();

    private CalculadoraMataMata(ConfigMataMata config, List<ParticipanteMotor> participantes,
            List<ResultadoMataMata> resultados, long seedSorteio) {
        this.config = config;
        this.seedSorteio = seedSorteio;
        this.idPorSeed = Seeds.porSeed(config.seeding(), participantes, seedSorteio);
        for (int i = 0; i < idPorSeed.size(); i++) {
            seedPorId.put(idPorSeed.get(i), i + 1);
        }
        for (ResultadoMataMata resultado : resultados) {
            ChaveJogo chave = new ChaveJogo(resultado.ordemFase(), resultado.posicao(), resultado.numeroJogo());
            if (naoEncaixados.putIfAbsent(chave, resultado) != null) {
                throw new ResultadoInvalidoException("Resultado duplicado para o " + descrever(chave));
            }
        }
    }

    /**
     * @param participantes já validados
     * @param resultados    já conferidos um a um, sem itens nulos
     */
    static EstadoMataMata calcular(ConfigMataMata config, List<ParticipanteMotor> participantes,
            List<ResultadoMataMata> resultados, long seedSorteio) {
        return new CalculadoraMataMata(config, participantes, resultados, seedSorteio).montar();
    }

    private EstadoMataMata montar() {
        int participantes = idPorSeed.size();
        int tamanho = Integer.highestOneBit(participantes - 1) * 2;
        int eliminatorias = Integer.numberOfTrailingZeros(tamanho);

        List<FaseEstado> fases = new ArrayList<>();
        FaseEstado anterior = null;
        FaseEstado terceiro = null;
        for (int numero = 1; numero <= eliminatorias; numero++) {
            int confrontos = tamanho >> numero;
            List<Vagas> vagas = numero == 1 ? vagasDaPrimeiraFase(tamanho) : vagasDaFaseSeguinte(anterior, confrontos);
            if (numero == eliminatorias) {
                if (haDisputaDeTerceiro(anterior)) {
                    terceiro = fase(numero, "Disputa de 3º lugar", TipoFase.TERCEIRO_LUGAR, vagasDoTerceiro(anterior));
                    fases.add(terceiro);
                }
                anterior = fase(fases.size() + 1, "Final", TipoFase.FINAL, vagas);
            } else {
                anterior = fase(numero, nomeDaFase(confrontos), TipoFase.ELIMINATORIA, vagas);
            }
            fases.add(anterior);
        }
        exigirTodosEncaixados();
        return estado(fases, anterior, terceiro);
    }

    // ---------------------------------------------------------------- vagas

    /** Confrontos da ordem clássica: a seed melhor é o A; seed acima de n é bye e deixa o B nulo. */
    private List<Vagas> vagasDaPrimeiraFase(int tamanho) {
        List<Integer> seeds = OrdemClassica.seeds(tamanho);
        List<Vagas> vagas = new ArrayList<>();
        for (int i = 0; i < seeds.size(); i += 2) {
            int melhor = seeds.get(i);
            int pior = seeds.get(i + 1);
            vagas.add(new Vagas(idPorSeed.get(melhor - 1), pior <= idPorSeed.size() ? idPorSeed.get(pior - 1) : null));
        }
        return vagas;
    }

    /**
     * Vaga de cada confronto da fase: o vencedor da posição {@code p} da fase anterior vai para a posição
     * {@code ceil(p/2)}. Com a fase anterior fechada, os dois vencedores são ordenados pela seed (o A é o melhor).
     * Enquanto ela não fecha, as vagas são {@code null}.
     */
    private List<Vagas> vagasDaFaseSeguinte(FaseEstado anterior, int confrontos) {
        if (!fechada(anterior)) {
            return Collections.nCopies(confrontos, null);
        }
        List<Vagas> vagas = new ArrayList<>();
        for (int p = 0; p < confrontos; p++) {
            vagas.add(ordenarPorSeed(anterior.confrontos().get(2 * p).vencedorId(),
                    anterior.confrontos().get(2 * p + 1).vencedorId()));
        }
        return vagas;
    }

    /** A disputa de 3º só existe com dois perdedores reais de semifinal: não há com um bye na semifinal. */
    private boolean haDisputaDeTerceiro(FaseEstado semifinal) {
        return config.disputaTerceiroLugar() && semifinal != null
                && semifinal.confrontos().stream().noneMatch(ConfrontoEstado::bye);
    }

    /** O confronto do 3º lugar: o perdedor da semifinal 1 contra o da semifinal 2, ou sem vagas se não fecharam. */
    private List<Vagas> vagasDoTerceiro(FaseEstado semifinal) {
        if (!fechada(semifinal)) {
            return Collections.nCopies(1, null);
        }
        return List.of(ordenarPorSeed(perdedor(semifinal.confrontos().get(0)), perdedor(semifinal.confrontos().get(1))));
    }

    private Vagas ordenarPorSeed(long um, long outro) {
        return seedPorId.get(um) < seedPorId.get(outro) ? new Vagas(um, outro) : new Vagas(outro, um);
    }

    private static long perdedor(ConfrontoEstado confronto) {
        return confronto.vencedorId().equals(confronto.participanteAId()) ? confronto.participanteBId()
                : confronto.participanteAId();
    }

    private static boolean fechada(FaseEstado fase) {
        return fase.confrontos().stream().allMatch(c -> c.vencedorId() != null);
    }

    // ---------------------------------------------------------------- fases e confrontos

    private FaseEstado fase(int ordem, String nome, TipoFase tipo, List<Vagas> vagas) {
        List<ConfrontoEstado> confrontos = new ArrayList<>();
        for (int i = 0; i < vagas.size(); i++) {
            confrontos.add(confronto(ordem, i + 1, vagas.get(i)));
        }
        return new FaseEstado(ordem, nome, tipo, confrontos);
    }

    private ConfrontoEstado confronto(int ordemFase, int posicao, Vagas vagas) {
        if (vagas != null && vagas.bye()) {
            return new ConfrontoEstado(posicao, vagas.a(), null, true, vagas.a(), List.of());
        }
        int quantidade = AvaliadorConfronto.quantidadeDeJogos(config);
        ResultadoMataMata[] porJogo = new ResultadoMataMata[quantidade];
        for (int numero = 1; numero <= quantidade; numero++) {
            porJogo[numero - 1] = naoEncaixados.remove(new ChaveJogo(ordemFase, posicao, numero));
            if (porJogo[numero - 1] != null && vagas == null) {
                throw new ResultadoInvalidoException("O " + descrever(new ChaveJogo(ordemFase, posicao, numero))
                        + " ainda aguarda os participantes (a fase anterior não terminou) e não aceita resultado");
            }
        }
        if (vagas == null) {
            return new ConfrontoEstado(posicao, null, null, false, null, jogos(null, null, quantidade, null));
        }
        AvaliadorConfronto.Avaliacao avaliacao = new AvaliadorConfronto(config, seedSorteio, ordemFase, posicao,
                vagas.a(), vagas.b()).avaliar(porJogo);
        return new ConfrontoEstado(posicao, vagas.a(), vagas.b(), false, avaliacao.vencedorId(),
                jogos(vagas.a(), vagas.b(), quantidade, avaliacao.situacoes()));
    }

    /** Jogos do confronto, com o mando de cada tipo. Sem situações, é um confronto que ainda aguarda os participantes. */
    private List<JogoEstado> jogos(Long a, Long b, int quantidade, List<SituacaoJogo> situacoes) {
        List<JogoEstado> jogos = new ArrayList<>();
        for (int numero = 1; numero <= quantidade; numero++) {
            boolean mandanteEhA = AvaliadorConfronto.mandanteEhA(config, numero);
            Long mandante = mandanteEhA ? a : b;
            Long visitante = mandanteEhA ? b : a;
            SituacaoJogo situacao = situacoes == null ? SituacaoJogo.AGUARDANDO_PARTICIPANTES : situacoes.get(numero - 1);
            jogos.add(new JogoEstado(numero, mandante, visitante, situacao));
        }
        return jogos;
    }

    /** Fase com {@code confrontos} confrontos: Semifinal, Quartas de final, Oitavas de final, "Fase de 32"... */
    private static String nomeDaFase(int confrontos) {
        return switch (confrontos) {
            case 2 -> "Semifinal";
            case 4 -> "Quartas de final";
            case 8 -> "Oitavas de final";
            default -> "Fase de " + 2 * confrontos;
        };
    }

    // ---------------------------------------------------------------- pódio e conferência

    private static EstadoMataMata estado(List<FaseEstado> fases, FaseEstado ultima, FaseEstado terceiro) {
        ConfrontoEstado decisao = ultima.confrontos().get(0);
        Long campeaoId = decisao.vencedorId();
        Long viceId = campeaoId == null ? null : perdedor(decisao);
        Long terceiroId = terceiro == null ? null : terceiro.confrontos().get(0).vencedorId();
        return new EstadoMataMata(fases, campeaoId, viceId, terceiroId);
    }

    private void exigirTodosEncaixados() {
        if (!naoEncaixados.isEmpty()) {
            ChaveJogo chave = naoEncaixados.keySet().iterator().next();
            throw new ResultadoInvalidoException("O " + descrever(chave)
                    + " não existe na chave (fase, confronto ou jogo inexistente, ou confronto de bye, que não tem jogos)");
        }
    }

    private static String descrever(ChaveJogo chave) {
        return "jogo " + chave.numeroJogo() + " do confronto " + chave.posicao() + " da fase " + chave.ordemFase();
    }
}
