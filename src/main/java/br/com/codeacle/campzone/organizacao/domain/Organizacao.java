package br.com.codeacle.campzone.organizacao.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.obrigatorio;
import static br.com.codeacle.campzone.common.domain.Invariantes.textoObrigatorio;

import java.time.Instant;

import br.com.codeacle.campzone.plano.domain.Plano;
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

/** A organização é o tenant: todo dado de negócio pertence a exatamente uma. */
@Entity
@Table(name = "organizacao", indexes = @Index(name = "idx_organizacao_plano", columnList = "plano_id"))
public class Organizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private Plano plano;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Organizacao() {
    }

    public Organizacao(String nome, Plano plano) {
        this.nome = textoObrigatorio(nome, "Nome da organização", 120);
        this.plano = obrigatorio(plano, "Plano");
        this.criadoEm = Instant.now();
    }

    public void renomear(String novoNome) {
        this.nome = textoObrigatorio(novoNome, "Nome da organização", 120);
    }

    public void trocarPlano(Plano novoPlano) {
        this.plano = obrigatorio(novoPlano, "Plano");
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Plano getPlano() {
        return plano;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
