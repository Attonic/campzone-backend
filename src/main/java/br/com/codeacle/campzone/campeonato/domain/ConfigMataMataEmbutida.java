package br.com.codeacle.campzone.campeonato.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

/**
 * Configuração do formato mata-mata (eliminatória simples), embutida em {@link Campeonato} (colunas com prefixo
 * {@code mm_}). Os campos são wrappers porque as colunas ficam nulas quando o campeonato é de outro formato.
 */
@Embeddable
public class ConfigMataMataEmbutida {

    /** Nome do {@link TipoConfronto}; texto simples para não gerar CHECK de enum (veja {@link Campeonato}). */
    @Column(name = "mm_tipo_confronto", length = 40)
    private String tipoConfronto;

    /** Número de jogos da série (ímpar, 3 ou mais). Só preenchido para {@link TipoConfronto#SERIE}. */
    @Column(name = "mm_melhor_de")
    private Integer melhorDe;

    /** Nome do {@link Seeding}; texto simples para não gerar CHECK de enum. */
    @Column(name = "mm_seeding", length = 40)
    private String seeding;

    @Column(name = "mm_disputa_terceiro_lugar")
    private Boolean disputaTerceiroLugar;

    @ElementCollection
    @CollectionTable(name = "campeonato_desempate_mata_mata",
            joinColumns = @JoinColumn(name = "campeonato_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "desempate", nullable = false, length = 40)
    private List<String> desempates = new ArrayList<>();

    protected ConfigMataMataEmbutida() {
    }

    public ConfigMataMataEmbutida(TipoConfronto tipoConfronto, Integer melhorDe, List<DesempateMataMata> desempates,
            Seeding seeding, boolean disputaTerceiroLugar) {
        obrigatorio(tipoConfronto, "Tipo de confronto");
        if (tipoConfronto == TipoConfronto.SERIE) {
            exigir(melhorDe != null && melhorDe >= 3 && melhorDe % 2 == 1,
                    "Em série, 'melhor de' deve ser um número ímpar maior ou igual a 3.");
        } else {
            exigir(melhorDe == null, "'Melhor de' só se aplica ao tipo de confronto SERIE.");
        }
        obrigatorio(desempates, "Desempates");
        exigir(!desempates.isEmpty(), "Informe ao menos um desempate.");
        exigir(desempates.stream().noneMatch(Objects::isNull), "Desempates não aceitam item nulo.");
        exigir(new HashSet<>(desempates).size() == desempates.size(), "Desempates não podem se repetir.");
        exigir(!desempates.contains(DesempateMataMata.GOLS_FORA) || tipoConfronto == TipoConfronto.IDA_E_VOLTA,
                "O desempate GOLS_FORA só vale para confrontos de ida e volta.");
        this.tipoConfronto = tipoConfronto.name();
        this.melhorDe = melhorDe;
        this.desempates = desempates.stream().map(Enum::name).collect(Collectors.toCollection(ArrayList::new));
        this.seeding = obrigatorio(seeding, "Seeding").name();
        this.disputaTerceiroLugar = disputaTerceiroLugar;
    }

    public TipoConfronto getTipoConfronto() {
        return TipoConfronto.valueOf(tipoConfronto);
    }

    /** Nulo quando o tipo de confronto não é {@link TipoConfronto#SERIE}. */
    public Integer getMelhorDe() {
        return melhorDe;
    }

    /** Desempates na ordem de aplicação (somente leitura). */
    public List<DesempateMataMata> getDesempates() {
        return desempates.stream().map(DesempateMataMata::valueOf).toList();
    }

    public Seeding getSeeding() {
        return Seeding.valueOf(seeding);
    }

    public boolean isDisputaTerceiroLugar() {
        return disputaTerceiroLugar;
    }
}
