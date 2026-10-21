package br.com.codeacle.campzone.participante.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;
import static br.com.codeacle.campzone.common.domain.Invariantes.textoObrigatorio;

import br.com.codeacle.campzone.campeonato.domain.Campeonato;
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

/** Participante de um campeonato (time, jogador, equipe). O nome é único dentro do campeonato. */
@Entity
@Table(name = "participante",
        uniqueConstraints = @UniqueConstraint(name = "uk_participante_campeonato_nome",
                columnNames = { "campeonato_id", "nome" }),
        indexes = @Index(name = "idx_participante_organizacao", columnList = "organizacao_id"))
public class Participante {

    public static final int TAMANHO_MAXIMO_NOME = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false, updatable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campeonato_id", nullable = false, updatable = false)
    private Campeonato campeonato;

    @Column(nullable = false, length = TAMANHO_MAXIMO_NOME)
    private String nome;

    /** Posição manual ou cabeça de chave (1 = melhor). Opcional. */
    @Column(name = "seed")
    private Integer seed;

    protected Participante() {
    }

    /** A organização é herdada do campeonato, o que impede um participante de outro tenant. */
    public Participante(Campeonato campeonato, String nome, Integer seed) {
        this.campeonato = obrigatorio(campeonato, "Campeonato");
        this.organizacao = campeonato.getOrganizacao();
        this.nome = textoObrigatorio(nome, "Nome do participante", TAMANHO_MAXIMO_NOME);
        this.seed = validarSeed(seed);
    }

    public void renomear(String novoNome) {
        this.nome = textoObrigatorio(novoNome, "Nome do participante", TAMANHO_MAXIMO_NOME);
    }

    public void definirSeed(Integer novaSeed) {
        this.seed = validarSeed(novaSeed);
    }

    /**
     * Identidade lógica: mesma instância ou mesmo id (cobre proxies LAZY do Hibernate, que não são a mesma
     * instância do objeto carregado).
     */
    public boolean mesmoQue(Participante outro) {
        if (outro == null) {
            return false;
        }
        if (this == outro) {
            return true;
        }
        return getId() != null && getId().equals(outro.getId());
    }

    private static Integer validarSeed(Integer seed) {
        exigir(seed == null || seed >= 1, "A seed do participante deve ser maior ou igual a 1.");
        return seed;
    }

    public Long getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public Campeonato getCampeonato() {
        return campeonato;
    }

    public String getNome() {
        return nome;
    }

    public Integer getSeed() {
        return seed;
    }
}
