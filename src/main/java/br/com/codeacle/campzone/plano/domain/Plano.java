package br.com.codeacle.campzone.plano.domain;

import static br.com.codeacle.campzone.common.domain.Invariantes.minimo;
import static br.com.codeacle.campzone.common.domain.Invariantes.textoObrigatorio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Plano comercial. Entidade global (não pertence a uma organização). Os limites aqui são a fonte da verdade e só
 * são lidos pelo {@code LimitePlanoService}.
 */
@Entity
@Table(name = "plano", uniqueConstraints = @UniqueConstraint(name = "uk_plano_codigo", columnNames = "codigo"))
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "max_campeonatos_ativos", nullable = false)
    private int maxCampeonatosAtivos;

    @Column(name = "max_participantes", nullable = false)
    private int maxParticipantes;

    protected Plano() {
    }

    public Plano(String codigo, String nome, int maxCampeonatosAtivos, int maxParticipantes) {
        this.codigo = textoObrigatorio(codigo, "Código do plano", 40);
        this.nome = textoObrigatorio(nome, "Nome do plano", 80);
        this.maxCampeonatosAtivos = minimo(maxCampeonatosAtivos, 0, "Máximo de campeonatos ativos");
        this.maxParticipantes = minimo(maxParticipantes, 0, "Máximo de participantes");
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public int getMaxCampeonatosAtivos() {
        return maxCampeonatosAtivos;
    }

    public int getMaxParticipantes() {
        return maxParticipantes;
    }
}
