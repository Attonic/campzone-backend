package br.com.codeacle.campzone.motor.pontoscorridos;

import java.util.List;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;
import br.com.codeacle.campzone.motor.FormatoCompeticao;
import br.com.codeacle.campzone.motor.OrdemSorteada;
import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesInvalidosException;
import br.com.codeacle.campzone.motor.ResultadoInvalidoException;
import br.com.codeacle.campzone.motor.ResultadoJogo;
import br.com.codeacle.campzone.motor.ValidadorParticipantes;

/**
 * Formato pontos corridos: todos contra todos em um ou mais turnos, com classificação por pontos e desempates
 * configuráveis. Sem estado e determinístico; veja o ADR 0005 e o Javadoc de {@link FormatoCompeticao}.
 */
public final class PontosCorridosFormato implements
        FormatoCompeticao<ConfigPontosCorridos, TabelaPontosCorridos, ResultadoJogo, Classificacao> {

    /**
     * Gera a tabela round-robin (método do círculo). A ordem da tabela é sorteada com {@code seedSorteio}; com
     * número ímpar de participantes há uma folga por rodada, nunca um jogo fantasma. O turno 2 repete o 1 com o
     * mando invertido, o 3 repete o 1, e assim por diante.
     *
     * @throws ConfigInvalidaException            se a config for nula
     * @throws ParticipantesInvalidosException    se os participantes forem inválidos
     */
    @Override
    public TabelaPontosCorridos gerar(ConfigPontosCorridos config, List<ParticipanteMotor> participantes,
            long seedSorteio) {
        exigirConfig(config);
        ValidadorParticipantes.validar(participantes);
        return GeradorTabela.gerar(config.turnos(), OrdemSorteada.de(participantes, seedSorteio));
    }

    /**
     * Rejeita o empate quando a config não o permite. Placar negativo e mandante igual ao visitante já são barrados
     * na criação do {@link ResultadoJogo}.
     *
     * @throws ResultadoInvalidoException se o resultado for nulo ou for um empate com {@code permiteEmpate = false}
     */
    @Override
    public void validarResultado(ConfigPontosCorridos config, ResultadoJogo resultado) {
        exigirConfig(config);
        if (resultado == null) {
            throw new ResultadoInvalidoException("O resultado é obrigatório");
        }
        if (!config.permiteEmpate() && resultado.placarMandante() == resultado.placarVisitante()) {
            throw new ResultadoInvalidoException("Este campeonato não permite empate: o jogo "
                    + resultado.mandanteId() + " x " + resultado.visitanteId() + " terminou "
                    + resultado.placarMandante() + " x " + resultado.placarVisitante());
        }
    }

    /**
     * Calcula a classificação com os jogos já disputados. Todo participante aparece, inclusive com 0 jogos.
     * Empatados que sobrevivem a todos os critérios dividem a posição e saem em ordem de id.
     *
     * @throws ResultadoInvalidoException se algum resultado for inválido ou citar participante desconhecido
     */
    @Override
    public Classificacao calcular(ConfigPontosCorridos config, List<ParticipanteMotor> participantes,
            List<ResultadoJogo> resultados, long seedSorteio) {
        exigirConfig(config);
        ValidadorParticipantes.validar(participantes);
        if (resultados == null) {
            throw new ResultadoInvalidoException("A lista de resultados é obrigatória");
        }
        List<Long> ordem = OrdemSorteada.de(participantes, seedSorteio);
        for (ResultadoJogo resultado : resultados) {
            validarResultado(config, resultado);
            exigirConhecido(ordem, resultado.mandanteId());
            exigirConhecido(ordem, resultado.visitanteId());
        }
        return CalculadoraClassificacao.calcular(config, ordem, resultados);
    }

    private static void exigirConfig(ConfigPontosCorridos config) {
        if (config == null) {
            throw new ConfigInvalidaException("A configuração é obrigatória");
        }
    }

    private static void exigirConhecido(List<Long> ids, long id) {
        if (!ids.contains(id)) {
            throw new ResultadoInvalidoException("Resultado com participante desconhecido: id " + id);
        }
    }
}
