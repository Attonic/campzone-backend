package br.com.codeacle.campzone.common.config;

/**
 * Ordem de execução dos seeders. Runners sem {@code @Order} rodam por último, então todos os seeders declaram a sua:
 * o plano {@code GRATIS} precisa existir antes de qualquer organização.
 */
public final class SeedOrdem {

    public static final int PLANO = 10;
    public static final int ADMIN = 20;
    public static final int DEV_DADOS = 30;

    private SeedOrdem() {
    }
}
