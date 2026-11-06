package br.com.codeacle.campzone.motor.pontoscorridos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.com.codeacle.campzone.motor.ResultadoJogo;

/**
 * Calcula a classificação: acumula as estatísticas e ordena aplicando os critérios na ordem configurada.
 *
 * <p>A ordenação parte de um único grupo (todos) e cada critério refina os grupos empatados em subgrupos, que
 * seguem para o critério seguinte. Por isso o confronto direto olha só os jogos entre os empatados do grupo naquele
 * momento (mini-tabela) e nunca é reaplicado a um subgrupo.
 */
final class CalculadoraClassificacao {

    private final ConfigPontosCorridos config;
    private final List<ResultadoJogo> resultados;
    private final Map<Long, Integer> posicaoNoSorteio = new HashMap<>();
    private final Map<Long, Desempenho> desempenhos = new TreeMap<>();

    private CalculadoraClassificacao(ConfigPontosCorridos config, List<Long> ordemSorteada,
            List<ResultadoJogo> resultados) {
        this.config = config;
        this.resultados = resultados;
        for (int i = 0; i < ordemSorteada.size(); i++) {
            Long id = ordemSorteada.get(i);
            posicaoNoSorteio.put(id, i);
            desempenhos.put(id, new Desempenho());
        }
    }

    /**
     * @param ordemSorteada ids de todos os participantes na ordem sorteada (a mesma da geração)
     * @param resultados    jogos já disputados e já validados
     */
    static Classificacao calcular(ConfigPontosCorridos config, List<Long> ordemSorteada,
            List<ResultadoJogo> resultados) {
        return new CalculadoraClassificacao(config, ordemSorteada, resultados).calcular();
    }

    private Classificacao calcular() {
        resultados.forEach(resultado -> registrar(desempenhos, resultado));
        List<LinhaClassificacao> linhas = new ArrayList<>();
        int posicao = 1;
        for (List<Long> grupo : ordenar()) {
            for (Long id : grupo) {
                linhas.add(linha(posicao, id, desempenhos.get(id)));
            }
            posicao += grupo.size();
        }
        return new Classificacao(linhas);
    }

    private LinhaClassificacao linha(int posicao, long id, Desempenho d) {
        return new LinhaClassificacao(posicao, id, pontos(d), d.jogos(), d.vitorias, d.empates, d.derrotas, d.golsPro,
                d.golsContra, d.golsPro - d.golsContra);
    }

    private int pontos(Desempenho d) {
        return d.vitorias * config.pontosVitoria() + d.empates * config.pontosEmpate()
                + d.derrotas * config.pontosDerrota();
    }

    /** Grupos de empatados em ordem de classificação; dentro de cada grupo, por id. */
    private List<List<Long>> ordenar() {
        List<List<Long>> grupos = List.of(new ArrayList<>(desempenhos.keySet()));
        for (CriterioDesempate criterio : config.criterios()) {
            List<List<Long>> refinados = new ArrayList<>();
            for (List<Long> grupo : grupos) {
                if (grupo.size() == 1) {
                    refinados.add(grupo);
                } else {
                    refinados.addAll(desempatar(criterio, grupo));
                }
            }
            grupos = refinados;
        }
        return grupos;
    }

    /** Separa o grupo empatado em subgrupos pelo critério (maior chave primeiro), mantendo a ordem por id. */
    private List<List<Long>> desempatar(CriterioDesempate criterio, List<Long> grupo) {
        Map<Long, Long> chaves = chaves(criterio, grupo);
        List<Long> ordenado = new ArrayList<>(grupo);
        ordenado.sort(Comparator.comparing((Long id) -> chaves.get(id)).reversed());
        List<List<Long>> subgrupos = new ArrayList<>();
        List<Long> atual = new ArrayList<>();
        for (Long id : ordenado) {
            if (!atual.isEmpty() && !chaves.get(id).equals(chaves.get(atual.get(0)))) {
                subgrupos.add(atual);
                atual = new ArrayList<>();
            }
            atual.add(id);
        }
        subgrupos.add(atual);
        return subgrupos;
    }

    /** Valor de cada participante do grupo no critério; maior é melhor. */
    private Map<Long, Long> chaves(CriterioDesempate criterio, List<Long> grupo) {
        Map<Long, Long> miniTabela = criterio == CriterioDesempate.CONFRONTO_DIRETO ? pontosNaMiniTabela(grupo)
                : Map.of();
        Map<Long, Long> chaves = new HashMap<>();
        for (Long id : grupo) {
            Desempenho d = desempenhos.get(id);
            chaves.put(id, switch (criterio) {
                case PONTOS -> (long) pontos(d);
                case VITORIAS -> (long) d.vitorias;
                case SALDO -> (long) (d.golsPro - d.golsContra);
                case PRO -> (long) d.golsPro;
                case CONFRONTO_DIRETO -> miniTabela.get(id);
                case SORTEIO -> (long) -posicaoNoSorteio.get(id);
            });
        }
        return chaves;
    }

    /** Pontos de cada membro do grupo contando só os jogos entre membros do grupo. */
    private Map<Long, Long> pontosNaMiniTabela(List<Long> grupo) {
        Map<Long, Desempenho> mini = new HashMap<>();
        grupo.forEach(id -> mini.put(id, new Desempenho()));
        for (ResultadoJogo resultado : resultados) {
            if (mini.containsKey(resultado.mandanteId()) && mini.containsKey(resultado.visitanteId())) {
                registrar(mini, resultado);
            }
        }
        Map<Long, Long> pontos = new HashMap<>();
        mini.forEach((id, d) -> pontos.put(id, (long) pontos(d)));
        return pontos;
    }

    private static void registrar(Map<Long, Desempenho> destino, ResultadoJogo resultado) {
        destino.get(resultado.mandanteId()).registrar(resultado.placarMandante(), resultado.placarVisitante());
        destino.get(resultado.visitanteId()).registrar(resultado.placarVisitante(), resultado.placarMandante());
    }

    /** Contadores acumulados de um participante. */
    private static final class Desempenho {
        private int vitorias;
        private int empates;
        private int derrotas;
        private int golsPro;
        private int golsContra;

        void registrar(int pro, int contra) {
            golsPro += pro;
            golsContra += contra;
            if (pro > contra) {
                vitorias++;
            } else if (pro == contra) {
                empates++;
            } else {
                derrotas++;
            }
        }

        int jogos() {
            return vitorias + empates + derrotas;
        }
    }
}
