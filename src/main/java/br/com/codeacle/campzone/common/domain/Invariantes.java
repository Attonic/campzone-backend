package br.com.codeacle.campzone.common.domain;

/** Verificações reutilizadas pelas entidades para garantir invariantes na construção e nas mudanças de estado. */
public final class Invariantes {

    private Invariantes() {
    }

    /** Lança {@link InvarianteDominioException} com a mensagem informada se a condição for falsa. */
    public static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new InvarianteDominioException(mensagem);
        }
    }

    public static <T> T obrigatorio(T valor, String campo) {
        exigir(valor != null, campo + " é obrigatório.");
        return valor;
    }

    /** Texto sem espaços nas pontas, obrigatório e com no máximo {@code tamanhoMaximo} caracteres. */
    public static String textoObrigatorio(String valor, String campo, int tamanhoMaximo) {
        exigir(valor != null && !valor.isBlank(), campo + " é obrigatório.");
        String aparado = valor.strip();
        exigir(aparado.length() <= tamanhoMaximo, campo + " deve ter no máximo " + tamanhoMaximo + " caracteres.");
        return aparado;
    }

    public static int minimo(int valor, int minimo, String campo) {
        exigir(valor >= minimo, campo + " deve ser maior ou igual a " + minimo + ".");
        return valor;
    }
}
