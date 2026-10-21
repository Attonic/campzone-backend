package br.com.codeacle.campzone.common.security;

/** Nomes dos claims do JWT do CampZone, em um só lugar para emissão e leitura. */
public final class TokenClaims {

    /** Papel do usuário ({@code ADMIN_PLATAFORMA} ou {@code ORGANIZADOR}). */
    public static final String PAPEL = "papel";

    /** Id da organização; só existe no token do organizador. */
    public static final String ORGANIZACAO = "org";

    /** Nome do usuário. */
    public static final String NOME = "nome";

    private TokenClaims() {
    }
}
