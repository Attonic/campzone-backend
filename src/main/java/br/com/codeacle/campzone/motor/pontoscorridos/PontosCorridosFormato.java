package br.com.codeacle.campzone.motor.pontoscorridos;

import java.util.List;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;
import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesInvalidosException;
import br.com.codeacle.campzone.motor.ResultadoInvalidoException;
import br.com.codeacle.campzone.motor.ResultadoJogo;
import br.com.codeacle.campzone.motor.ValidadorParticipantes;

/**
 * Formato pontos corridos: todos contra todos em um ou mais turnos, com classificação por pontos e desempates
 * configuráveis. Sem estado e determinístico (ADR 0005).
 */
public final class PontosCorridosFormato {

    /**
     * Gera a tabela round-robin (método do círculo). A ordem da tabela é sorteada com {@code seedSorteio}; com
     * número ímpar de participantes há uma folga por rodada, nunca um jogo fantasma. O turno 2 repete o 1 com o
     * mando invertido, o 3 repete o 1, e assim por diante.
     *
     * @throws ConfigInvalidaException            se a config for nula
     * @throws ParticipantesInvalidosException    se os participantes forem inválidos
     */
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

    private static void exigirConfig(ConfigPontosCorridos config) {
        if (config == null) {
            throw new ConfigInvalidaException("A configuração é obrigatória");
        }
    }
}
