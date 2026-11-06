package br.com.codeacle.campzone.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import br.com.codeacle.campzone.auth.domain.Usuario;
import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.campeonato.domain.StatusCampeonato;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.plano.domain.Plano;
import br.com.codeacle.campzone.support.Cenario;
import br.com.codeacle.campzone.support.RepositorioTest;

/** As consultas cruzadas do admin: contagens por organização, status ativos, ordenação, busca e paginação. */
@RepositorioTest
class AdminConsultaRepositoryTest {

    private static final List<String> ATIVOS = List.of(StatusCampeonato.RASCUNHO.name(),
            StatusCampeonato.EM_ANDAMENTO.name());
    private static final Instant BASE = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AdminConsultaRepository repository;

    /** Prefixo único por teste: isola as organizações do teste de qualquer outra no banco. */
    private final String prefixo = "T" + UUID.randomUUID().toString().substring(0, 8) + " ";

    private Organizacao organizacao(String nome, Instant criadoEm) {
        Plano plano = Cenario.plano(em);
        Organizacao organizacao = em.persist(new Organizacao(prefixo + nome, plano));
        em.flush();
        em.getEntityManager().createNativeQuery("update organizacao set criado_em = :criadoEm where id = :id")
                .setParameter("criadoEm", criadoEm).setParameter("id", organizacao.getId()).executeUpdate();
        em.clear();
        return organizacao;
    }

    private void organizador(Organizacao organizacao) {
        Organizacao gerenciada = em.find(Organizacao.class, organizacao.getId());
        em.persist(Usuario.organizador("Fulano", "u" + UUID.randomUUID() + "@x.com", "hash", gerenciada));
    }

    private void campeonato(Organizacao organizacao, StatusCampeonato status) {
        Organizacao gerenciada = em.find(Organizacao.class, organizacao.getId());
        Campeonato campeonato = Cenario.pontosCorridos(em, gerenciada);
        if (status != StatusCampeonato.RASCUNHO) {
            campeonato.iniciar();
        }
        if (status == StatusCampeonato.FINALIZADO) {
            campeonato.finalizar();
        }
    }

    private List<OrganizacaoComContagens> listar(String padrao, int pagina, int tamanho) {
        em.flush();
        em.clear();
        return repository.listarOrganizacoes(padrao, ATIVOS, PageRequest.of(pagina, tamanho));
    }

    @Test
    void contagensGlobaisSomamTudoEFinalizadoNaoContaComoAtivo() {
        long orgs = repository.contarOrganizacoes();
        long usuarios = repository.contarUsuarios();
        long campeonatos = repository.contarCampeonatos();
        long ativos = repository.contarCampeonatosPorStatus(ATIVOS);

        Organizacao a = organizacao("A", BASE);
        Organizacao b = organizacao("B", BASE.plusSeconds(1));
        organizador(a);
        organizador(a);
        organizador(b);
        em.persist(Usuario.adminPlataforma("Admin", "adm" + UUID.randomUUID() + "@x.com", "hash"));
        campeonato(a, StatusCampeonato.RASCUNHO);
        campeonato(a, StatusCampeonato.EM_ANDAMENTO);
        campeonato(a, StatusCampeonato.FINALIZADO);
        campeonato(b, StatusCampeonato.FINALIZADO);
        em.flush();
        em.clear();

        assertThat(repository.contarOrganizacoes()).isEqualTo(orgs + 2);
        assertThat(repository.contarUsuarios()).isEqualTo(usuarios + 4);
        assertThat(repository.contarCampeonatos()).isEqualTo(campeonatos + 4);
        assertThat(repository.contarCampeonatosPorStatus(ATIVOS)).isEqualTo(ativos + 2);
    }

    @Test
    void contagensPorOrganizacaoNaoMisturamOutrasOrganizacoes() {
        Organizacao a = organizacao("A", BASE);
        Organizacao b = organizacao("B", BASE.plusSeconds(1));
        Organizacao vazia = organizacao("Vazia", BASE.plusSeconds(2));
        organizador(a);
        organizador(a);
        organizador(b);
        campeonato(a, StatusCampeonato.RASCUNHO);
        campeonato(a, StatusCampeonato.EM_ANDAMENTO);
        campeonato(a, StatusCampeonato.FINALIZADO);
        campeonato(b, StatusCampeonato.FINALIZADO);
        campeonato(b, StatusCampeonato.FINALIZADO);

        List<OrganizacaoComContagens> lista = listar("%" + prefixo + "%", 0, 10);

        assertThat(lista).extracting(OrganizacaoComContagens::id).containsExactly(vazia.getId(), b.getId(),
                a.getId());
        OrganizacaoComContagens resultadoA = lista.get(2);
        assertThat(resultadoA.totalUsuarios()).isEqualTo(2);
        assertThat(resultadoA.totalCampeonatos()).isEqualTo(3);
        assertThat(resultadoA.campeonatosAtivos()).isEqualTo(2);
        OrganizacaoComContagens resultadoB = lista.get(1);
        assertThat(resultadoB.totalUsuarios()).isEqualTo(1);
        assertThat(resultadoB.totalCampeonatos()).isEqualTo(2);
        assertThat(resultadoB.campeonatosAtivos()).isZero();
        OrganizacaoComContagens resultadoVazia = lista.get(0);
        assertThat(resultadoVazia.totalUsuarios()).isZero();
        assertThat(resultadoVazia.totalCampeonatos()).isZero();
        assertThat(resultadoVazia.campeonatosAtivos()).isZero();
    }

    @Test
    void trazNomeEPlanoDaOrganizacaoEACriacaoGravada() {
        Organizacao a = organizacao("Liga", BASE);

        OrganizacaoComContagens lido = listar("%" + prefixo + "%", 0, 10).get(0);

        Plano plano = em.find(Organizacao.class, a.getId()).getPlano();
        assertThat(lido.id()).isEqualTo(a.getId());
        assertThat(lido.nome()).isEqualTo(prefixo + "Liga");
        assertThat(lido.planoCodigo()).isEqualTo(plano.getCodigo());
        assertThat(lido.planoNome()).isEqualTo("Plano de teste");
        assertThat(lido.criadoEm()).isEqualTo(BASE);
    }

    @Test
    void ordenaPorCriacaoDecrescenteEDesempataPorIdDecrescente() {
        Organizacao velha = organizacao("Velha", BASE);
        Organizacao empateA = organizacao("EmpateA", BASE.plusSeconds(60));
        Organizacao empateB = organizacao("EmpateB", BASE.plusSeconds(60));
        Organizacao nova = organizacao("Nova", BASE.plusSeconds(120));

        List<OrganizacaoComContagens> lista = listar("%" + prefixo + "%", 0, 10);

        assertThat(empateB.getId()).isGreaterThan(empateA.getId());
        assertThat(lista).extracting(OrganizacaoComContagens::id)
                .containsExactly(nova.getId(), empateB.getId(), empateA.getId(), velha.getId());
    }

    @Test
    void paginaEmCimaDaOrdenacaoSemRepetirNemPerder() {
        for (int i = 0; i < 5; i++) {
            organizacao("Org" + i, BASE.plusSeconds(i));
        }
        em.flush();
        em.clear();

        List<Long> todas = listar("%" + prefixo + "%", 0, 10).stream().map(OrganizacaoComContagens::id).toList();
        List<Long> pagina0 = listar("%" + prefixo + "%", 0, 2).stream().map(OrganizacaoComContagens::id).toList();
        List<Long> pagina1 = listar("%" + prefixo + "%", 1, 2).stream().map(OrganizacaoComContagens::id).toList();
        List<Long> pagina2 = listar("%" + prefixo + "%", 2, 2).stream().map(OrganizacaoComContagens::id).toList();
        List<Long> pagina3 = listar("%" + prefixo + "%", 3, 2).stream().map(OrganizacaoComContagens::id).toList();

        assertThat(todas).hasSize(5);
        assertThat(pagina0).containsExactlyElementsOf(todas.subList(0, 2));
        assertThat(pagina1).containsExactlyElementsOf(todas.subList(2, 4));
        assertThat(pagina2).containsExactlyElementsOf(todas.subList(4, 5));
        assertThat(pagina3).isEmpty();
        assertThat(repository.contarOrganizacoesPorNome("%" + prefixo + "%")).isEqualTo(5);
    }

    @Test
    void buscaPorNomeNaoDiferenciaMaiusculasEContemOTexto() {
        Organizacao liga = organizacao("Liga dos Amigos", BASE);
        organizacao("Copa Society", BASE.plusSeconds(1));
        organizacao("Clube de Xadrez", BASE.plusSeconds(2));

        assertThat(listar("%" + prefixo.toLowerCase() + "%amigos%", 0, 10))
                .extracting(OrganizacaoComContagens::id).containsExactly(liga.getId());
        assertThat(listar("%" + prefixo + "LIGA%", 0, 10))
                .extracting(OrganizacaoComContagens::id).containsExactly(liga.getId());
        assertThat(listar("%" + prefixo + "%e%", 0, 10)).hasSize(2);
        assertThat(repository.contarOrganizacoesPorNome("%" + prefixo.toUpperCase() + "%society%")).isEqualTo(1);
        assertThat(listar("%" + prefixo + "inexistente%", 0, 10)).isEmpty();
        assertThat(repository.contarOrganizacoesPorNome("%" + prefixo + "inexistente%")).isZero();
    }

    @Test
    void caracteresCuringaDoLikeSaoLiteraisQuandoEscapados() {
        Organizacao comPorcento = organizacao("100% Futebol", BASE);
        organizacao("100 Futebol", BASE.plusSeconds(1));
        Organizacao comUnderline = organizacao("Time_A", BASE.plusSeconds(2));
        organizacao("TimeXA", BASE.plusSeconds(3));

        assertThat(listar("%" + prefixo + "100!% futebol%", 0, 10))
                .extracting(OrganizacaoComContagens::id).containsExactly(comPorcento.getId());
        assertThat(listar("%" + prefixo + "time!_a%", 0, 10))
                .extracting(OrganizacaoComContagens::id).containsExactly(comUnderline.getId());
        // Sem escape, o underline casa com qualquer caractere: prova que o escape é o que torna literal.
        assertThat(listar("%" + prefixo + "time_a%", 0, 10)).hasSize(2);
    }
}
