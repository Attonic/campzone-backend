package br.com.codeacle.campzone.partida.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;

import java.time.Instant;

import org.hibernate.annotations.Check;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Resultado de uma {@link Partida} (1:1, a partida existe antes). O placar tem nome genérico porque vale para gols,
 * pontos ou vitórias. O placar de desempate (pênaltis ou equivalente) é opcional e vem sempre em par.
 */
@Entity
@Table(name = "resultado",
        uniqueConstraints = @UniqueConstraint(name = "uk_resultado_partida", columnNames = "partida_id"),
        indexes = @Index(name = "idx_resultado_organizacao", columnList = "organizacao_id"))
@Check(name = "ck_resultado_placares", constraints = "placar_mandante >= 0 AND placar_visitante >= 0 "
        + "AND (placar_desempate_mandante IS NULL OR placar_desempate_mandante >= 0) "
        + "AND (placar_desempate_visitante IS NULL OR placar_desempate_visitante >= 0)")
public class Resultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false, updatable = false)
    private Organizacao organizacao;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false, updatable = false)
    private Partida partida;

    @Column(name = "placar_mandante", nullable = false)
    private int placarMandante;

    @Column(name = "placar_visitante", nullable = false)
    private int placarVisitante;

    @Column(name = "placar_desempate_mandante")
    private Integer placarDesempateMandante;

    @Column(name = "placar_desempate_visitante")
    private Integer placarDesempateVisitante;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    @Version
    private long version;

    protected Resultado() {
    }

    /** A organização é herdada da partida. O desempate é opcional (ambos nulos ou ambos informados). */
    public Resultado(Partida partida, int placarMandante, int placarVisitante, Integer placarDesempateMandante,
            Integer placarDesempateVisitante) {
        this.partida = obrigatorio(partida, "Partida");
        this.organizacao = partida.getOrganizacao();
        definirPlacar(placarMandante, placarVisitante, placarDesempateMandante, placarDesempateVisitante);
        this.registradoEm = Instant.now();
    }

    /** Resultado sem placar de desempate. */
    public Resultado(Partida partida, int placarMandante, int placarVisitante) {
        this(partida, placarMandante, placarVisitante, null, null);
    }

    /** Corrige o placar de um resultado já registrado. */
    public void corrigir(int placarMandante, int placarVisitante, Integer placarDesempateMandante,
            Integer placarDesempateVisitante) {
        definirPlacar(placarMandante, placarVisitante, placarDesempateMandante, placarDesempateVisitante);
    }

    private void definirPlacar(int placarMandante, int placarVisitante, Integer desempateMandante,
            Integer desempateVisitante) {
        exigir(placarMandante >= 0 && placarVisitante >= 0, "O placar não pode ser negativo.");
        exigir((desempateMandante == null) == (desempateVisitante == null),
                "O placar de desempate deve ser informado para os dois lados ou para nenhum.");
        exigir(desempateMandante == null || (desempateMandante >= 0 && desempateVisitante >= 0),
                "O placar de desempate não pode ser negativo.");
        this.placarMandante = placarMandante;
        this.placarVisitante = placarVisitante;
        this.placarDesempateMandante = desempateMandante;
        this.placarDesempateVisitante = desempateVisitante;
    }

    public Long getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public Partida getPartida() {
        return partida;
    }

    public int getPlacarMandante() {
        return placarMandante;
    }

    public int getPlacarVisitante() {
        return placarVisitante;
    }

    public Integer getPlacarDesempateMandante() {
        return placarDesempateMandante;
    }

    public Integer getPlacarDesempateVisitante() {
        return placarDesempateVisitante;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public long getVersion() {
        return version;
    }
}
