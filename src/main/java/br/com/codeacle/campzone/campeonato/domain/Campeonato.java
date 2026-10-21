package br.com.codeacle.campzone.campeonato.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.exigir;
import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;
import static br.com.codeacle.campzone.common.domain.Invariantes.textoObrigatorio;

import java.time.Instant;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Campeonato de qualquer modalidade (a modalidade é só um rótulo). Só a configuração do formato escolhido é
 * preenchida; a outra fica nula. Campeonato "ativo" (limite do plano) é {@code RASCUNHO} ou {@code EM_ANDAMENTO}.
 */
@Entity
@Table(name = "campeonato",
        uniqueConstraints = @UniqueConstraint(name = "uk_campeonato_slug", columnNames = "slug"),
        indexes = @Index(name = "idx_campeonato_organizacao", columnList = "organizacao_id"))
public class Campeonato {

    public static final int TAMANHO_MAXIMO_NOME = 120;
    public static final int TAMANHO_MAXIMO_SLUG = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false, updatable = false)
    private Organizacao organizacao;

    @Column(nullable = false, length = TAMANHO_MAXIMO_NOME)
    private String nome;

    /** Texto livre, ex.: "Futebol society". */
    @Column(nullable = false, length = 60)
    private String modalidade;

    /** Identificador da URL pública ({@code /c/{slug}}), único global. A geração fica no módulo de serviço. */
    @Column(nullable = false, length = TAMANHO_MAXIMO_SLUG)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private FormatoCampeonato formato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCampeonato status;

    @Column(nullable = false)
    private boolean publico;

    @Embedded
    private ConfigPontosCorridosEmbutida configPontosCorridos;

    @Embedded
    private ConfigMataMataEmbutida configMataMata;

    /** Seed do sorteio, persistida para torná-lo reprodutível e auditável. Nula até a geração. */
    @Column(name = "seed_sorteio")
    private Long seedSorteio;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Version
    private long version;

    protected Campeonato() {
    }

    private Campeonato(Organizacao organizacao, String nome, String modalidade, String slug, FormatoCampeonato formato,
            ConfigPontosCorridosEmbutida configPontosCorridos, ConfigMataMataEmbutida configMataMata) {
        this.organizacao = obrigatorio(organizacao, "Organização");
        this.nome = textoObrigatorio(nome, "Nome do campeonato", TAMANHO_MAXIMO_NOME);
        this.modalidade = textoObrigatorio(modalidade, "Modalidade", 60);
        this.slug = textoObrigatorio(slug, "Slug", TAMANHO_MAXIMO_SLUG);
        this.formato = obrigatorio(formato, "Formato");
        switch (formato) {
            case PONTOS_CORRIDOS -> {
                exigir(configPontosCorridos != null, "Formato PONTOS_CORRIDOS exige a configuração de pontos corridos.");
                exigir(configMataMata == null, "Formato PONTOS_CORRIDOS não aceita configuração de mata-mata.");
            }
            case MATA_MATA -> {
                exigir(configMataMata != null, "Formato MATA_MATA exige a configuração de mata-mata.");
                exigir(configPontosCorridos == null, "Formato MATA_MATA não aceita configuração de pontos corridos.");
            }
        }
        this.configPontosCorridos = configPontosCorridos;
        this.configMataMata = configMataMata;
        this.status = StatusCampeonato.RASCUNHO;
        this.publico = false;
        this.criadoEm = Instant.now();
    }

    public static Campeonato pontosCorridos(Organizacao organizacao, String nome, String modalidade, String slug,
            ConfigPontosCorridosEmbutida config) {
        return new Campeonato(organizacao, nome, modalidade, slug, FormatoCampeonato.PONTOS_CORRIDOS, config, null);
    }

    public static Campeonato mataMata(Organizacao organizacao, String nome, String modalidade, String slug,
            ConfigMataMataEmbutida config) {
        return new Campeonato(organizacao, nome, modalidade, slug, FormatoCampeonato.MATA_MATA, null, config);
    }

    /** Fábrica genérica: a configuração informada deve corresponder ao formato (e a outra ser nula). */
    public static Campeonato criar(Organizacao organizacao, String nome, String modalidade, String slug,
            FormatoCampeonato formato, ConfigPontosCorridosEmbutida configPontosCorridos,
            ConfigMataMataEmbutida configMataMata) {
        return new Campeonato(organizacao, nome, modalidade, slug, formato, configPontosCorridos, configMataMata);
    }

    public void renomear(String novoNome) {
        this.nome = textoObrigatorio(novoNome, "Nome do campeonato", TAMANHO_MAXIMO_NOME);
    }

    public void alterarModalidade(String novaModalidade) {
        this.modalidade = textoObrigatorio(novaModalidade, "Modalidade", 60);
    }

    public void definirPublico(boolean publico) {
        this.publico = publico;
    }

    /** Registra a seed do sorteio; só pode ser definida uma vez, para manter a auditoria. */
    public void registrarSeedSorteio(long seed) {
        exigir(this.seedSorteio == null, "A seed do sorteio já foi registrada.");
        this.seedSorteio = seed;
    }

    public void iniciar() {
        exigir(status == StatusCampeonato.RASCUNHO, "Só é possível iniciar um campeonato em rascunho.");
        this.status = StatusCampeonato.EM_ANDAMENTO;
    }

    public void finalizar() {
        exigir(status == StatusCampeonato.EM_ANDAMENTO, "Só é possível finalizar um campeonato em andamento.");
        this.status = StatusCampeonato.FINALIZADO;
    }

    public boolean isAtivo() {
        return status == StatusCampeonato.RASCUNHO || status == StatusCampeonato.EM_ANDAMENTO;
    }

    public Long getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public String getNome() {
        return nome;
    }

    public String getModalidade() {
        return modalidade;
    }

    public String getSlug() {
        return slug;
    }

    public FormatoCampeonato getFormato() {
        return formato;
    }

    public StatusCampeonato getStatus() {
        return status;
    }

    public boolean isPublico() {
        return publico;
    }

    /**
     * Nulo se o formato não for pontos corridos. O teste do formato é necessário porque, ao ler do banco, o
     * Hibernate nunca devolve nulo para um embeddable que contém coleção (cria um objeto vazio); a fonte da verdade
     * é o {@code formato}.
     */
    public ConfigPontosCorridosEmbutida getConfigPontosCorridos() {
        return formato == FormatoCampeonato.PONTOS_CORRIDOS ? configPontosCorridos : null;
    }

    /** Nulo se o formato não for mata-mata (mesma observação de {@link #getConfigPontosCorridos()}). */
    public ConfigMataMataEmbutida getConfigMataMata() {
        return formato == FormatoCampeonato.MATA_MATA ? configMataMata : null;
    }

    public Long getSeedSorteio() {
        return seedSorteio;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public long getVersion() {
        return version;
    }
}
