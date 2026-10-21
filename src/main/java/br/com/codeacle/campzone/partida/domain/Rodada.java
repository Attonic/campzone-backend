package br.com.codeacle.campzone.partida.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.minimo;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;

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

/** Rodada de uma fase de pontos corridos. */
@Entity
@Table(name = "rodada",
        uniqueConstraints = @UniqueConstraint(name = "uk_rodada_fase_numero", columnNames = { "fase_id", "numero" }),
        indexes = @Index(name = "idx_rodada_organizacao", columnList = "organizacao_id"))
public class Rodada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false, updatable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fase_id", nullable = false, updatable = false)
    private Fase fase;

    @Column(nullable = false)
    private int numero;

    protected Rodada() {
    }

    /** A organização é herdada da fase. */
    public Rodada(Fase fase, int numero) {
        this.fase = obrigatorio(fase, "Fase");
        this.organizacao = fase.getOrganizacao();
        this.numero = minimo(numero, 1, "Número da rodada");
    }

    public Long getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public Fase getFase() {
        return fase;
    }

    public int getNumero() {
        return numero;
    }
}
