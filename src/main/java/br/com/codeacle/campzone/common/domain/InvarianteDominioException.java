package br.com.codeacle.campzone.common.domain;

/**
 * Violação de uma invariante do domínio (ex.: nome vazio, placar negativo). Indica entrada inválida que escapou
 * das validações das bordas ou erro de programação; não deve ser usada para erros de negócio esperados.
 */
public class InvarianteDominioException extends IllegalArgumentException {

    public InvarianteDominioException(String mensagem) {
        super(mensagem);
    }
}
