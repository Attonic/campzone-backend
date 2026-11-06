package br.com.codeacle.campzone.motor;

import java.util.List;

/**
 * Strategy de um formato de competição (ADR 0005). Cada formato tem sua config tipada, sua estrutura gerada e seu
 * estado calculado, por isso a interface é genérica. Implementações são Java puro, sem estado e determinísticas:
 * as mesmas entradas e a mesma {@code seedSorteio} dão sempre a mesma saída.
 *
 * @param <C> configuração do formato
 * @param <E> estrutura gerada (tabela de jogos, chave...)
 * @param <S> estado calculado a partir dos resultados (classificação, avanço de fase...)
 */
public interface FormatoCompeticao<C, E, S> {

    /**
     * Gera a estrutura inicial de jogos.
     *
     * @param seedSorteio semente de todo sorteio; é gerada e persistida pela camada de aplicação
     */
    E gerar(C config, List<ParticipanteMotor> participantes, long seedSorteio);

    /**
     * Confere se o resultado é aceito pela config do formato.
     *
     * @throws ResultadoInvalidoException se o resultado for rejeitado
     */
    void validarResultado(C config, ResultadoJogo resultado);

    /**
     * Calcula o estado da competição a partir dos jogos já disputados.
     *
     * @param resultados só os jogos disputados
     */
    S calcular(C config, List<ParticipanteMotor> participantes, List<ResultadoJogo> resultados, long seedSorteio);
}
