package br.com.codeacle.campzone.support;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.campeonato.domain.ConfigMataMataEmbutida;
import br.com.codeacle.campzone.campeonato.domain.ConfigPontosCorridosEmbutida;
import br.com.codeacle.campzone.campeonato.domain.DesempateMataMata;
import br.com.codeacle.campzone.campeonato.domain.Seeding;
import br.com.codeacle.campzone.campeonato.domain.TipoConfronto;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.participante.domain.Participante;
import br.com.codeacle.campzone.plano.domain.Plano;

/** Fábricas de dados persistidos para os testes de repositório. */
public final class Cenario {

    private Cenario() {
    }

    public static Plano plano(TestEntityManager em) {
        return em.persist(new Plano("T" + UUID.randomUUID(), "Plano de teste", 1, 8));
    }

    public static Organizacao organizacao(TestEntityManager em, String nome) {
        return em.persist(new Organizacao(nome, plano(em)));
    }

    public static String slugUnico() {
        return "camp-" + UUID.randomUUID();
    }

    public static Campeonato pontosCorridos(TestEntityManager em, Organizacao organizacao) {
        return em.persist(Campeonato.pontosCorridos(organizacao, "Liga", "Futebol society", slugUnico(),
                ConfigPontosCorridosEmbutida.padrao(2)));
    }

    public static Campeonato mataMata(TestEntityManager em, Organizacao organizacao) {
        return em.persist(Campeonato.mataMata(organizacao, "Copa", "Xadrez", slugUnico(),
                new ConfigMataMataEmbutida(TipoConfronto.JOGO_UNICO, null, List.of(DesempateMataMata.PENALTIS),
                        Seeding.ALEATORIO, false)));
    }

    public static Participante participante(TestEntityManager em, Campeonato campeonato, String nome) {
        return em.persist(new Participante(campeonato, nome, null));
    }
}
