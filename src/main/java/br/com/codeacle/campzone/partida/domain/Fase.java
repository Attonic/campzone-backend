package br.com.codeacle.campzone.partida.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.minimo;
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
import jakarta.persistence.Version;

/** Fase do campeonato (ex.: "Fase única" nos pontos corridos; "Semifinal" no mata-mata). */
@Entity
@Table(name = "fase",
        uniqueConstraints = @UniqueConstraint(name = "uk_fase_campeonato_ordem",
                columnNames = { "campeonato_id", "ordem" }),
        indexes = @Index(name = "idx_fase_organizacao", columnList = "organizacao_id"))
public class Fase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false, updatable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campeonato_id", nullable = false, updatable = false)
    private Campeonato campeonato;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false)
    private int ordem;

    /** Nome do {@link StatusFase}; texto simples para não gerar CHECK de enum (veja {@code Campeonato.formato}). */
    @Column(nullable = false, length = 40)
    private String status;

    @Version
    private Long version;

    protected Fase() {
    }

    /** A organização é herdada do campeonato. */
    public Fase(Campeonato campeonato, String nome, int ordem) {
        this.campeonato = obrigatorio(campeonato, "Campeonato");
        this.organizacao = campeonato.getOrganizacao();
        this.nome = textoObrigatorio(nome, "Nome da fase", 80);
        this.ordem = minimo(ordem, 1, "Ordem da fase");
        this.status = StatusFase.PENDENTE.name();
    }

    public void iniciar() {
        exigir(getStatus() == StatusFase.PENDENTE, "Só é possível iniciar uma fase pendente.");
        this.status = StatusFase.EM_ANDAMENTO.name();
    }

    public void encerrar() {
        exigir(getStatus() == StatusFase.EM_ANDAMENTO, "Só é possível encerrar uma fase em andamento.");
        this.status = StatusFase.ENCERRADA.name();
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

    public int getOrdem() {
        return ordem;
    }

    public StatusFase getStatus() {
        return StatusFase.valueOf(status);
    }

    public Long getVersion() {
        return version;
    }
}
