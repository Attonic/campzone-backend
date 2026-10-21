package br.com.codeacle.campzone.auth.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;
import static br.com.codeacle.campzone.common.domain.Invariantes.textoObrigatorio;

import java.time.Instant;
import java.util.Locale;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Usuário que faz login. O {@code ORGANIZADOR} pertence a exatamente uma organização; o {@code ADMIN_PLATAFORMA}
 * não pertence a nenhuma (ADR 0002). Essa invariante é garantida pelas fábricas, e a coluna {@code organizacao_id}
 * é anulável de propósito (ADR 0003: a obrigatoriedade fica no domínio).
 */
@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"),
        indexes = @Index(name = "idx_usuario_organizacao", columnList = "organizacao_id"))
public class Usuario {

    public static final int EMAIL_TAMANHO_MAXIMO = 254;
    public static final int NOME_TAMANHO_MAXIMO = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NOME_TAMANHO_MAXIMO)
    private String nome;

    @Column(nullable = false, length = EMAIL_TAMANHO_MAXIMO)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    /** Enum gravado como texto, sem CHECK (ADR 0003). */
    @Column(nullable = false, length = 40)
    private String papel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id")
    private Organizacao organizacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Usuario() {
    }

    private Usuario(String nome, String email, String senhaHash, Papel papel, Organizacao organizacao) {
        this.nome = textoObrigatorio(nome, "Nome", NOME_TAMANHO_MAXIMO);
        this.email = normalizarEmail(email);
        this.senhaHash = textoObrigatorio(senhaHash, "Hash da senha", 255);
        this.papel = obrigatorio(papel, "Papel").name();
        this.organizacao = organizacao;
        this.criadoEm = Instant.now();
    }

    /** Organizador: obrigatoriamente vinculado a uma organização. */
    public static Usuario organizador(String nome, String email, String senhaHash, Organizacao organizacao) {
        obrigatorio(organizacao, "Organização do organizador");
        return new Usuario(nome, email, senhaHash, Papel.ORGANIZADOR, organizacao);
    }

    /** Admin da plataforma: nunca vinculado a uma organização. */
    public static Usuario adminPlataforma(String nome, String email, String senhaHash) {
        return new Usuario(nome, email, senhaHash, Papel.ADMIN_PLATAFORMA, null);
    }

    /** E-mail sem espaços nas pontas e em minúsculas: a unicidade é global e não diferencia caixa. */
    public static String normalizarEmail(String email) {
        String normalizado = textoObrigatorio(email, "E-mail", EMAIL_TAMANHO_MAXIMO).toLowerCase(Locale.ROOT);
        exigir(normalizado.indexOf('@') > 0 && normalizado.indexOf('@') < normalizado.length() - 1,
                "E-mail inválido.");
        return normalizado;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Papel getPapel() {
        return Papel.valueOf(papel);
    }

    /** Nula para o admin da plataforma. */
    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
