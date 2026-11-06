package br.com.codeacle.campzone.motor.matamata;

import java.util.ArrayList;
import java.util.List;

import br.com.codeacle.campzone.motor.ResultadoInvalidoException;

/**
 * Avalia um confronto: dado quem são A e B e os resultados dos seus jogos (por número do jogo, {@code null} onde ainda
 * não há resultado), diz quem venceu e a situação de cada jogo, e rejeita os resultados que não cabem no confronto.
 *
 * <p>Os desempates valem na ordem configurada e só quando necessários. Um jogo que não precisa de prorrogação ou
 * pênaltis não os aceita, e um jogo empatado sem os dados que o desempate exige é rejeitado. Os jogos de um confronto
 * são lançados em ordem: um jogo só aceita resultado depois de todos os anteriores, porque a validade dele (e a
 * existência dos jogos seguintes) depende deles.
 */
final class AvaliadorConfronto {

    /**
     * Resultado da avaliação.
     *
     * @param vencedorId {@code null} se o confronto ainda não foi decidido
     * @param situacoes  situação de cada jogo, na ordem dos jogos
     */
    record Avaliacao(Long vencedorId, List<SituacaoJogo> situacoes) {
    }

    /**
     * O que um desempate precisa saber do confronto (ou do jogo, na série) em disputa, do ponto de vista de A e B.
     *
     * @param rotulo            o que se decide ("jogo 1 do confronto 2 da fase 1" ou "agregado do confronto..."), para
     *                          as mensagens de erro
     * @param a                 id do participante A
     * @param b                 id do participante B
     * @param placarA           placar de A no tempo normal (o agregado, na ida e volta)
     * @param placarB           placar de B no tempo normal (o agregado, na ida e volta)
     * @param golsForaA         gols de A como visitante; {@code null} quando o critério não se aplica
     * @param golsForaB         gols de B como visitante; {@code null} quando o critério não se aplica
     * @param decisivo          o jogo que carrega a prorrogação e os pênaltis (o jogo 2, na ida e volta)
     * @param mandanteEhA       se A é o mandante do jogo decisivo
     * @param sorteioFavoreceA  resultado do sorteio do confronto, usado só se o desempate chegar a ele
     */
    record Disputa(String rotulo, long a, long b, long placarA, long placarB, Integer golsForaA, Integer golsForaB,
            ResultadoMataMata decisivo, boolean mandanteEhA, boolean sorteioFavoreceA) {

        Integer prorrogacaoA() {
            return mandanteEhA ? decisivo.prorrogacaoMandante() : decisivo.prorrogacaoVisitante();
        }

        Integer prorrogacaoB() {
            return mandanteEhA ? decisivo.prorrogacaoVisitante() : decisivo.prorrogacaoMandante();
        }

        Integer penaltisA() {
            return mandanteEhA ? decisivo.penaltisMandante() : decisivo.penaltisVisitante();
        }

        Integer penaltisB() {
            return mandanteEhA ? decisivo.penaltisVisitante() : decisivo.penaltisMandante();
        }
    }

    /** O que os desempates já usaram dos dados do jogo decisivo. */
    private static final class Consumo {
        private boolean prorrogacao;
        private boolean penaltis;
    }

    private final ConfigMataMata config;
    private final long a;
    private final long b;
    private final int ordemFase;
    private final int posicao;
    private final boolean sorteioFavoreceA;

    AvaliadorConfronto(ConfigMataMata config, long seedSorteio, int ordemFase, int posicao, long a, long b) {
        this.config = config;
        this.a = a;
        this.b = b;
        this.ordemFase = ordemFase;
        this.posicao = posicao;
        this.sorteioFavoreceA = Sorteio.favoreceA(seedSorteio, ordemFase, posicao);
    }

    /** Quantidade de jogos de um confronto com participantes (sem contar o bye). */
    static int quantidadeDeJogos(ConfigMataMata config) {
        return switch (config.tipoConfronto()) {
            case JOGO_UNICO -> 1;
            case IDA_E_VOLTA -> 2;
            case SERIE -> config.melhorDe();
        };
    }

    /** Se A é o mandante do jogo: A no jogo único e nos ímpares da série; só na volta da ida e volta. */
    static boolean mandanteEhA(ConfigMataMata config, int numeroJogo) {
        return switch (config.tipoConfronto()) {
            case JOGO_UNICO -> true;
            case IDA_E_VOLTA -> numeroJogo == 2;
            case SERIE -> numeroJogo % 2 == 1;
        };
    }

    /**
     * @param porJogo resultados por jogo (índice 0 = jogo 1), com {@code null} onde não há resultado
     * @throws ResultadoInvalidoException se um resultado não couber no confronto
     */
    Avaliacao avaliar(ResultadoMataMata[] porJogo) {
        return switch (config.tipoConfronto()) {
            case JOGO_UNICO -> jogoUnico(porJogo[0]);
            case IDA_E_VOLTA -> idaEVolta(porJogo[0], porJogo[1]);
            case SERIE -> serie(porJogo);
        };
    }

    // ---------------------------------------------------------------- jogo único

    private Avaliacao jogoUnico(ResultadoMataMata jogo) {
        if (jogo == null) {
            return new Avaliacao(null, List.of(SituacaoJogo.PENDENTE));
        }
        Disputa disputa = new Disputa(rotulo(jogo), a, b, jogo.placarMandante(), jogo.placarVisitante(), null, null,
                jogo, true, sorteioFavoreceA);
        return new Avaliacao(decidir(config.desempates(), disputa), List.of(SituacaoJogo.DISPUTADO));
    }

    // ---------------------------------------------------------------- ida e volta

    /** A ida tem B como mandante, a volta tem A. Decide o agregado; prorrogação e pênaltis ficam no jogo 2. */
    private Avaliacao idaEVolta(ResultadoMataMata ida, ResultadoMataMata volta) {
        if (ida == null) {
            if (volta != null) {
                throw new ResultadoInvalidoException("O " + rotulo(volta)
                        + " ainda não aceita resultado: lance antes o jogo de ida (jogo 1)");
            }
            return new Avaliacao(null, List.of(SituacaoJogo.PENDENTE, SituacaoJogo.PENDENTE));
        }
        if (ida.temProrrogacao() || ida.temPenaltis()) {
            throw new ResultadoInvalidoException("O " + rotulo(ida)
                    + " não aceita prorrogação nem pênaltis: eles valem só no jogo 2, quando o agregado empata");
        }
        if (volta == null) {
            return new Avaliacao(null, List.of(SituacaoJogo.DISPUTADO, SituacaoJogo.PENDENTE));
        }
        // A é visitante na ida e mandante na volta; B, o contrário
        long agregadoA = (long) ida.placarVisitante() + volta.placarMandante();
        long agregadoB = (long) ida.placarMandante() + volta.placarVisitante();
        Disputa disputa = new Disputa("agregado do confronto " + posicao + " da fase " + ordemFase, a, b, agregadoA,
                agregadoB, ida.placarVisitante(), volta.placarVisitante(), volta, true, sorteioFavoreceA);
        return new Avaliacao(decidir(config.desempates(), disputa),
                List.of(SituacaoJogo.DISPUTADO, SituacaoJogo.DISPUTADO));
    }

    // ---------------------------------------------------------------- série

    /**
     * Cada jogo precisa ter vencedor (o empate aplica os desempates do próprio jogo). Quem chega a
     * {@code (melhorDe + 1) / 2} vitórias vence, e os jogos que sobram ficam desnecessários.
     */
    private Avaliacao serie(ResultadoMataMata[] porJogo) {
        int necessarias = (config.melhorDe() + 1) / 2;
        int vitoriasA = 0;
        int vitoriasB = 0;
        Long vencedor = null;
        boolean faltaJogoAnterior = false;
        List<SituacaoJogo> situacoes = new ArrayList<>();
        for (int numero = 1; numero <= porJogo.length; numero++) {
            ResultadoMataMata jogo = porJogo[numero - 1];
            if (vencedor != null) {
                if (jogo != null) {
                    throw new ResultadoInvalidoException("O " + rotulo(jogo)
                            + " é desnecessário e não aceita resultado: a série já foi decidida antes dele");
                }
                situacoes.add(SituacaoJogo.DESNECESSARIO);
            } else if (jogo == null) {
                faltaJogoAnterior = true;
                situacoes.add(SituacaoJogo.PENDENTE);
            } else {
                if (faltaJogoAnterior) {
                    throw new ResultadoInvalidoException("O " + rotulo(jogo)
                            + " ainda não aceita resultado: lance antes os jogos anteriores da série");
                }
                boolean mandanteEhA = mandanteEhA(config, numero);
                long placarA = mandanteEhA ? jogo.placarMandante() : jogo.placarVisitante();
                long placarB = mandanteEhA ? jogo.placarVisitante() : jogo.placarMandante();
                Disputa disputa = new Disputa(rotulo(jogo), a, b, placarA, placarB, null, null, jogo, mandanteEhA,
                        sorteioFavoreceA);
                if (decidir(config.desempates(), disputa) == a) {
                    vitoriasA++;
                } else {
                    vitoriasB++;
                }
                situacoes.add(SituacaoJogo.DISPUTADO);
                if (vitoriasA == necessarias) {
                    vencedor = a;
                } else if (vitoriasB == necessarias) {
                    vencedor = b;
                }
            }
        }
        return new Avaliacao(vencedor, situacoes);
    }

    // ---------------------------------------------------------------- desempates

    /**
     * Decide a disputa: o placar, se não empatou; senão cada desempate configurado, em ordem, até um decidir. Depois,
     * confere que o resultado não trouxe prorrogação ou pênaltis que o jogo não precisou.
     *
     * @return o id do vencedor (A ou B)
     * @throws ResultadoInvalidoException se faltarem dados do desempate, se os pênaltis empatarem ou se sobrarem dados
     */
    static long decidir(List<DesempateMataMata> criterios, Disputa disputa) {
        Consumo consumo = new Consumo();
        Long vencedor = null;
        if (disputa.placarA() != disputa.placarB()) {
            vencedor = disputa.placarA() > disputa.placarB() ? disputa.a() : disputa.b();
        }
        for (int i = 0; vencedor == null && i < criterios.size(); i++) {
            vencedor = aplicar(criterios.get(i), disputa, consumo);
        }
        if (vencedor == null) {
            throw new ResultadoInvalidoException("O " + disputa.rotulo() + " terminou empatado e nenhum critério de "
                    + "desempate o decidiu");
        }
        if (disputa.prorrogacaoA() != null && !consumo.prorrogacao) {
            throw new ResultadoInvalidoException("O " + disputa.rotulo()
                    + " não precisa da prorrogação informada: já estava decidido antes dela, ou o critério PRORROGACAO "
                    + "não vem antes do que o decidiu");
        }
        if (disputa.penaltisA() != null && !consumo.penaltis) {
            throw new ResultadoInvalidoException("O " + disputa.rotulo()
                    + " não precisa dos pênaltis informados: já estava decidido antes deles, ou o critério PENALTIS "
                    + "não vem antes do que o decidiu");
        }
        return vencedor;
    }

    /** @return o vencedor se o critério decidiu, ou {@code null} se empatou ou não se aplica */
    private static Long aplicar(DesempateMataMata criterio, Disputa disputa, Consumo consumo) {
        return switch (criterio) {
            case GOLS_FORA -> golsFora(disputa);
            case PRORROGACAO -> prorrogacao(disputa, consumo);
            case PENALTIS -> penaltis(disputa, consumo);
            case SORTEIO -> disputa.sorteioFavoreceA() ? disputa.a() : disputa.b();
        };
    }

    /** Não se aplica fora da ida e volta (sem gols fora na disputa), e então é pulado. */
    private static Long golsFora(Disputa disputa) {
        if (disputa.golsForaA() == null) {
            return null;
        }
        return maior(disputa.golsForaA(), disputa.golsForaB(), disputa);
    }

    private static Long prorrogacao(Disputa disputa, Consumo consumo) {
        if (disputa.prorrogacaoA() == null) {
            throw new ResultadoInvalidoException("O " + disputa.rotulo()
                    + " terminou empatado e o próximo desempate é a prorrogação: informe a prorrogação");
        }
        consumo.prorrogacao = true;
        return maior(disputa.prorrogacaoA(), disputa.prorrogacaoB(), disputa);
    }

    private static Long penaltis(Disputa disputa, Consumo consumo) {
        if (disputa.penaltisA() == null) {
            throw new ResultadoInvalidoException("O " + disputa.rotulo()
                    + " terminou empatado e o próximo desempate são os pênaltis: informe os pênaltis");
        }
        consumo.penaltis = true;
        Long vencedor = maior(disputa.penaltisA(), disputa.penaltisB(), disputa);
        if (vencedor == null) {
            throw new ResultadoInvalidoException("Os pênaltis do " + disputa.rotulo()
                    + " não podem terminar empatados (" + disputa.penaltisA() + " x " + disputa.penaltisB() + ")");
        }
        return vencedor;
    }

    /** O lado com o maior valor, ou {@code null} se empatam. */
    private static Long maior(int valorA, int valorB, Disputa disputa) {
        if (valorA == valorB) {
            return null;
        }
        return valorA > valorB ? disputa.a() : disputa.b();
    }

    private String rotulo(ResultadoMataMata jogo) {
        return "jogo " + jogo.numeroJogo() + " do confronto " + posicao + " da fase " + ordemFase;
    }
}
