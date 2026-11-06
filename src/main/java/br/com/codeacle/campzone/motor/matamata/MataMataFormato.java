package br.com.codeacle.campzone.motor.matamata;

import java.util.List;

import br.com.codeacle.campzone.motor.ConfigInvalidaException;
import br.com.codeacle.campzone.motor.FormatoCompeticao;
import br.com.codeacle.campzone.motor.ParticipanteMotor;
import br.com.codeacle.campzone.motor.ParticipantesInvalidosException;
import br.com.codeacle.campzone.motor.ResultadoInvalidoException;
import br.com.codeacle.campzone.motor.ValidadorParticipantes;

/**
 * Formato mata-mata (eliminatória simples): chave com byes e seeding, confrontos em jogo único, ida e volta ou série,
 * desempates configuráveis, disputa de 3º lugar opcional e avanço de fase. Sem estado e determinístico; veja o ADR
 * 0005 (com o Adendo A) e o Javadoc de {@link FormatoCompeticao}.
 *
 * <p>O estado da chave é uma função pura: {@code gerar} equivale a {@code calcular} sem resultados, e quem está em
 * cada vaga, o vencedor de cada confronto e os jogos abertos são sempre recalculados a partir dos resultados.
 */
public final class MataMataFormato implements
        FormatoCompeticao<ConfigMataMata, EstadoMataMata, ResultadoMataMata, EstadoMataMata> {

    /**
     * Gera a chave: todas as fases, com a primeira já preenchida (com byes para as melhores seeds) e as seguintes
     * aguardando os vencedores. Equivale a {@link #calcular} sem resultados.
     *
     * @throws ConfigInvalidaException         se a config for nula
     * @throws ParticipantesInvalidosException se os participantes ou as seeds forem inválidos
     */
    @Override
    public EstadoMataMata gerar(ConfigMataMata config, List<ParticipanteMotor> participantes, long seedSorteio) {
        return calcular(config, participantes, List.of(), seedSorteio);
    }

    /**
     * Confere o que dá para saber de um resultado sem os outros: a prorrogação e os pênaltis só podem vir se o critério
     * correspondente está na config, e os pênaltis não podem empatar. Placar negativo e pares incompletos já são
     * barrados na criação do {@link ResultadoMataMata}.
     *
     * @throws ConfigInvalidaException    se a config for nula
     * @throws ResultadoInvalidoException se o resultado for nulo ou violar uma dessas regras
     */
    @Override
    public void validarResultado(ConfigMataMata config, ResultadoMataMata resultado) {
        exigirConfig(config);
        if (resultado == null) {
            throw new ResultadoInvalidoException("O resultado é obrigatório");
        }
        if (resultado.temProrrogacao() && !config.desempates().contains(DesempateMataMata.PRORROGACAO)) {
            throw new ResultadoInvalidoException("O resultado do " + descrever(resultado)
                    + " traz prorrogação, mas PRORROGACAO não está entre os desempates configurados");
        }
        if (resultado.temPenaltis()) {
            if (!config.desempates().contains(DesempateMataMata.PENALTIS)) {
                throw new ResultadoInvalidoException("O resultado do " + descrever(resultado)
                        + " traz pênaltis, mas PENALTIS não está entre os desempates configurados");
            }
            if (resultado.penaltisMandante().equals(resultado.penaltisVisitante())) {
                throw new ResultadoInvalidoException("Os pênaltis do " + descrever(resultado)
                        + " não podem terminar empatados (" + resultado.penaltisMandante() + " x "
                        + resultado.penaltisVisitante() + ")");
            }
        }
    }

    /**
     * Calcula o estado da chave com os jogos já disputados: quem ocupa cada vaga, o vencedor de cada confronto, a
     * situação de cada jogo e o pódio. Cada resultado é validado no contexto dos demais (jogo existente e aberto,
     * sem duplicata, na ordem dos jogos do confronto, com os dados de desempate que o jogo exige). Para validar um
     * resultado novo, chame este método com ele incluído; a ordem da lista não muda o estado.
     *
     * @throws ConfigInvalidaException         se a config for nula
     * @throws ParticipantesInvalidosException se os participantes ou as seeds forem inválidos
     * @throws ResultadoInvalidoException      se a lista for nula ou algum resultado for inválido no contexto
     */
    @Override
    public EstadoMataMata calcular(ConfigMataMata config, List<ParticipanteMotor> participantes,
            List<ResultadoMataMata> resultados, long seedSorteio) {
        exigirConfig(config);
        ValidadorParticipantes.validar(participantes);
        if (resultados == null) {
            throw new ResultadoInvalidoException("A lista de resultados é obrigatória");
        }
        for (ResultadoMataMata resultado : resultados) {
            validarResultado(config, resultado);
        }
        return CalculadoraMataMata.calcular(config, participantes, resultados, seedSorteio);
    }

    private static void exigirConfig(ConfigMataMata config) {
        if (config == null) {
            throw new ConfigInvalidaException("A configuração é obrigatória");
        }
    }

    private static String descrever(ResultadoMataMata resultado) {
        return "jogo " + resultado.numeroJogo() + " do confronto " + resultado.posicao() + " da fase "
                + resultado.ordemFase();
    }
}
