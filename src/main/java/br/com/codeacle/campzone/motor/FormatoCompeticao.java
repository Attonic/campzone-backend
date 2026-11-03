package br.com.codeacle.campzone.motor;

import java.util.List;

/**
 * Strategy de um formato de competição (ADR 0005 e Adendo A). Cada formato tem sua config tipada, sua estrutura
 * gerada, o tipo do seu resultado e seu estado calculado, por isso a interface é genérica. Implementações são Java
 * puro, sem estado e determinísticas: as mesmas entradas e a mesma {@code seedSorteio} dão sempre a mesma saída.
 *
 * @param <C> configuração do formato
 * @param <E> estrutura gerada (tabela de jogos, chave...)
 * @param <R> resultado de um jogo disputado (no pontos corridos, {@link ResultadoJogo})
 * @param <S> estado calculado a partir dos resultados (classificação, avanço de fase...)
 */
public interface FormatoCompeticao<C, E, R, S> {

    /**
     * Gera a estrutura inicial de jogos. No mata-mata, em que a estrutura e o estado são o mesmo tipo (ADR 0005,
     * Adendo A), equivale a {@link #calcular} sem nenhum resultado.
     *
     * @param seedSorteio semente de todo sorteio; é gerada e persistida pela camada de aplicação
     */
    E gerar(C config, List<ParticipanteMotor> participantes, long seedSorteio);

    /**
     * Confere se o resultado é aceito pela config do formato, só com as checagens que não dependem de outros
     * resultados. Onde a validade depende dos demais (no mata-mata, por exemplo, um empate que o jogo decisivo não
     * aceita), a conferência completa é do {@link #calcular}: para validar um resultado novo, chame-o com ele incluído.
     *
     * @throws ResultadoInvalidoException se o resultado for rejeitado
     */
    void validarResultado(C config, R resultado);

    /**
     * Calcula o estado da competição a partir dos jogos já disputados, validando cada resultado no contexto.
     *
     * @param resultados só os jogos disputados
     * @throws ResultadoInvalidoException se algum resultado for inválido no contexto dos demais
     */
    S calcular(C config, List<ParticipanteMotor> participantes, List<R> resultados, long seedSorteio);
}
