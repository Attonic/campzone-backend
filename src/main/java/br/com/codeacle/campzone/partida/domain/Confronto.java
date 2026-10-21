package br.com.codeacle.campzone.partida.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.minimo;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;

import br.com.codeacle.campzone.campeonato.domain.FormatoCampeonato;
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
 * Confronto do mata-mata: agrupa de 1 a N partidas (jogo único, ida e volta, série), tem posição na chave e pode
 * ser bye (participante avança sem jogar). As colunas dos participantes ficam anuláveis no banco (o
 * {@code ddl-auto=update} não remove NOT NULL depois), mas as fábricas exigem os participantes.
 */
@Entity
@Table(name = "confronto",
        uniqueConstraints = @UniqueConstraint(name = "uk_confronto_fase_posicao",
                columnNames = { "fase_id", "posicao" }),
        indexes = @Index(name = "idx_confronto_organizacao", columnList = "organizacao_id"))
public class Confronto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false, updatable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fase_id", nullable = false, updatable = false)
    private Fase fase;

    /** Posição do confronto na chave (1 = topo). */
    @Column(nullable = false)
    private int posicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_a_id")
    private Participante participanteA;

    /** Nulo quando o confronto é bye. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_b_id")
    private Participante participanteB;

    /** Nulo até o confronto ser decidido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vencedor_id")
    private Participante vencedor;

    @Column(nullable = false)
    private boolean bye;

    @Version
    private Long version;

    protected Confronto() {
    }

    private Confronto(Fase fase, int posicao, Participante participanteA, Participante participanteB, boolean bye) {
        this.fase = obrigatorio(fase, "Fase");
        exigir(fase.getCampeonato().getFormato() == FormatoCampeonato.MATA_MATA,
                "Confrontos só existem em campeonatos de mata-mata.");
        this.organizacao = fase.getOrganizacao();
        this.posicao = minimo(posicao, 1, "Posição do confronto");
        this.participanteA = obrigatorio(participanteA, "Participante A");
        exigir(participanteA.pertenceAo(fase.getCampeonato()), "Participante A não pertence ao campeonato da fase.");
        exigir(participanteB == null || participanteB.pertenceAo(fase.getCampeonato()),
                "Participante B não pertence ao campeonato da fase.");
        this.participanteB = participanteB;
        this.bye = bye;
        if (bye) {
            this.vencedor = participanteA;
        }
    }

    /** Confronto entre dois participantes distintos. */
    public static Confronto entre(Fase fase, int posicao, Participante participanteA, Participante participanteB) {
        obrigatorio(participanteB, "Participante B");
        obrigatorio(participanteA, "Participante A");
        exigir(!Participante.mesmo(participanteA, participanteB),
                "Um confronto precisa de dois participantes diferentes.");
        return new Confronto(fase, posicao, participanteA, participanteB, false);
    }

    /** Bye: o participante avança sem jogar e já é o vencedor. */
    public static Confronto comBye(Fase fase, int posicao, Participante participante) {
        return new Confronto(fase, posicao, participante, null, true);
    }

    /** Define o vencedor, que deve ser um dos dois participantes. Não se aplica a bye. */
    public void definirVencedor(Participante vencedor) {
        exigir(!bye, "Confronto bye já tem o vencedor definido.");
        obrigatorio(vencedor, "Vencedor");
        exigir(Participante.mesmo(vencedor, participanteA) || Participante.mesmo(vencedor, participanteB),
                "O vencedor deve ser um dos participantes do confronto.");
        this.vencedor = vencedor;
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

    public int getPosicao() {
        return posicao;
    }

    public Participante getParticipanteA() {
        return participanteA;
    }

    public Participante getParticipanteB() {
        return participanteB;
    }

    public Participante getVencedor() {
        return vencedor;
    }

    public boolean isBye() {
        return bye;
    }

    public Long getVersion() {
        return version;
    }
}
