package br.com.codeacle.campzone.partida.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.minimo;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;

import java.time.Instant;

import org.hibernate.annotations.Check;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.participante.domain.Participante;
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

/**
 * Uma partida entre dois participantes. Pertence a uma {@link Rodada} (pontos corridos) ou a um {@link Confronto}
 * (mata-mata), nunca aos dois. O resultado vive em {@link Resultado}, criado depois.
 */
@Entity
@Check(name = "ck_partida_rodada_xor_confronto", constraints = "(rodada_id IS NULL) <> (confronto_id IS NULL)")
@Table(name = "partida",
        uniqueConstraints = @UniqueConstraint(name = "uk_partida_confronto_numero_jogo",
                columnNames = { "confronto_id", "numero_jogo" }),
        indexes = {
                @Index(name = "idx_partida_organizacao", columnList = "organizacao_id"),
                @Index(name = "idx_partida_fase", columnList = "fase_id"),
                @Index(name = "idx_partida_rodada", columnList = "rodada_id"),
                @Index(name = "idx_partida_confronto", columnList = "confronto_id") })
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false, updatable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fase_id", nullable = false, updatable = false)
    private Fase fase;

    /** Nulo no mata-mata. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rodada_id", updatable = false)
    private Rodada rodada;

    /** Nulo nos pontos corridos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confronto_id", updatable = false)
    private Confronto confronto;

    /** Posição da partida dentro do confronto (1..N). Nos pontos corridos é sempre 1. */
    @Column(name = "numero_jogo", nullable = false)
    private int numeroJogo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandante_id", nullable = false)
    private Participante mandante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitante_id", nullable = false)
    private Participante visitante;

    /** Nome do {@link StatusPartida}; texto simples para não gerar CHECK de enum (veja {@code Campeonato.formato}). */
    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "data_hora")
    private Instant dataHora;

    @Version
    private Long version;

    protected Partida() {
    }

    private Partida(Fase fase, Rodada rodada, Confronto confronto, int numeroJogo, Participante mandante,
            Participante visitante) {
        this.fase = obrigatorio(fase, "Fase");
        obrigatorio(mandante, "Mandante");
        obrigatorio(visitante, "Visitante");
        exigir(mandante.pertenceAo(fase.getCampeonato()) && visitante.pertenceAo(fase.getCampeonato()),
                "Mandante e visitante devem pertencer ao campeonato da fase.");
        this.organizacao = fase.getOrganizacao();
        exigir((rodada == null) != (confronto == null), "A partida pertence a uma rodada ou a um confronto, não aos dois.");
        this.rodada = rodada;
        this.confronto = confronto;
        this.numeroJogo = minimo(numeroJogo, 1, "Número do jogo");
        this.mandante = obrigatorio(mandante, "Mandante");
        this.visitante = obrigatorio(visitante, "Visitante");
        exigir(!Participante.mesmo(mandante, visitante), "Mandante e visitante devem ser participantes diferentes.");
        this.status = StatusPartida.AGENDADA.name();
    }

    /** Partida de pontos corridos. */
    public static Partida daRodada(Rodada rodada, Participante mandante, Participante visitante) {
        obrigatorio(rodada, "Rodada");
        return new Partida(rodada.getFase(), rodada, null, 1, mandante, visitante);
    }

    /** Partida de um confronto do mata-mata ({@code numeroJogo} de 1 a N dentro do confronto). */
    public static Partida doConfronto(Confronto confronto, int numeroJogo, Participante mandante,
            Participante visitante) {
        obrigatorio(confronto, "Confronto");
        obrigatorio(mandante, "Mandante");
        obrigatorio(visitante, "Visitante");
        exigir(!confronto.isBye(), "Um confronto bye não tem partidas.");
        boolean mandoNormal = Participante.mesmo(mandante, confronto.getParticipanteA())
                && Participante.mesmo(visitante, confronto.getParticipanteB());
        boolean mandoInvertido = Participante.mesmo(mandante, confronto.getParticipanteB())
                && Participante.mesmo(visitante, confronto.getParticipanteA());
        exigir(mandoNormal || mandoInvertido, "Mandante e visitante devem ser os participantes do confronto.");
        return new Partida(confronto.getFase(), null, confronto, numeroJogo, mandante, visitante);
    }

    public void agendar(Instant dataHora) {
        exigir(getStatus() == StatusPartida.AGENDADA, "Não é possível reagendar uma partida encerrada.");
        this.dataHora = dataHora;
    }

    public void encerrar() {
        exigir(getStatus() == StatusPartida.AGENDADA, "A partida já está encerrada.");
        this.status = StatusPartida.ENCERRADA.name();
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

    public Rodada getRodada() {
        return rodada;
    }

    public Confronto getConfronto() {
        return confronto;
    }

    public int getNumeroJogo() {
        return numeroJogo;
    }

    public Participante getMandante() {
        return mandante;
    }

    public Participante getVisitante() {
        return visitante;
    }

    public StatusPartida getStatus() {
        return StatusPartida.valueOf(status);
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public Long getVersion() {
        return version;
    }
}
