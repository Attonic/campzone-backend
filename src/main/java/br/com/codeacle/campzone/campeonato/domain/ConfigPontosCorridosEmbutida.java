package br.com.codeacle.campzone.campeonato.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.minimo;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

/**
 * Configuração do formato pontos corridos, embutida em {@link Campeonato} (colunas com prefixo {@code pc_}).
 * Os campos são wrappers porque as colunas ficam nulas quando o campeonato é de outro formato.
 */
@Embeddable
public class ConfigPontosCorridosEmbutida {

    static final int PONTOS_VITORIA_PADRAO = 3;
    static final int PONTOS_EMPATE_PADRAO = 1;
    static final int PONTOS_DERROTA_PADRAO = 0;

    @Column(name = "pc_turnos")
    private Integer turnos;

    @Column(name = "pc_pontos_vitoria")
    private Integer pontosVitoria;

    @Column(name = "pc_pontos_empate")
    private Integer pontosEmpate;

    @Column(name = "pc_pontos_derrota")
    private Integer pontosDerrota;

    @Column(name = "pc_permite_empate")
    private Boolean permiteEmpate;

    @ElementCollection
    @CollectionTable(name = "campeonato_criterio_desempate",
            joinColumns = @JoinColumn(name = "campeonato_id"))
    @OrderColumn(name = "posicao")
    @Enumerated(EnumType.STRING)
    @Column(name = "criterio", nullable = false, length = 30)
    private List<CriterioDesempate> criteriosDesempate = new ArrayList<>();

    protected ConfigPontosCorridosEmbutida() {
    }

    public ConfigPontosCorridosEmbutida(int turnos, int pontosVitoria, int pontosEmpate, int pontosDerrota,
            boolean permiteEmpate, List<CriterioDesempate> criteriosDesempate) {
        this.turnos = minimo(turnos, 1, "Turnos");
        this.pontosVitoria = pontosVitoria;
        this.pontosEmpate = pontosEmpate;
        this.pontosDerrota = pontosDerrota;
        this.permiteEmpate = permiteEmpate;
        obrigatorio(criteriosDesempate, "Critérios de desempate");
        exigir(criteriosDesempate.stream().noneMatch(Objects::isNull), "Critérios de desempate não aceitam item nulo.");
        exigir(new HashSet<>(criteriosDesempate).size() == criteriosDesempate.size(),
                "Critérios de desempate não podem se repetir.");
        this.criteriosDesempate = new ArrayList<>(criteriosDesempate);
    }

    /** Pontuação padrão 3/1/0, empate permitido e critérios na ordem usual de campeonatos. */
    public static ConfigPontosCorridosEmbutida padrao(int turnos) {
        return new ConfigPontosCorridosEmbutida(turnos, PONTOS_VITORIA_PADRAO, PONTOS_EMPATE_PADRAO,
                PONTOS_DERROTA_PADRAO, true,
                List.of(CriterioDesempate.PONTOS, CriterioDesempate.VITORIAS, CriterioDesempate.SALDO,
                        CriterioDesempate.PRO, CriterioDesempate.CONFRONTO_DIRETO, CriterioDesempate.SORTEIO));
    }

    public int getTurnos() {
        return turnos;
    }

    public int getPontosVitoria() {
        return pontosVitoria;
    }

    public int getPontosEmpate() {
        return pontosEmpate;
    }

    public int getPontosDerrota() {
        return pontosDerrota;
    }

    public boolean isPermiteEmpate() {
        return permiteEmpate;
    }

    /** Critérios na ordem de aplicação (somente leitura). */
    public List<CriterioDesempate> getCriteriosDesempate() {
        return Collections.unmodifiableList(criteriosDesempate);
    }
}
