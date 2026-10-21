package br.com.codeacle.campzone.campeonato.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.campeonato.domain.ConfigMataMataEmbutida;
import br.com.codeacle.campzone.campeonato.domain.ConfigPontosCorridosEmbutida;
import br.com.codeacle.campzone.campeonato.domain.CriterioDesempate;
import br.com.codeacle.campzone.campeonato.domain.DesempateMataMata;
import br.com.codeacle.campzone.campeonato.domain.FormatoCampeonato;
import br.com.codeacle.campzone.campeonato.domain.Seeding;
import br.com.codeacle.campzone.campeonato.domain.StatusCampeonato;
import br.com.codeacle.campzone.campeonato.domain.TipoConfronto;
import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.support.Cenario;
import br.com.codeacle.campzone.support.RepositorioTest;

@RepositorioTest
class CampeonatoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CampeonatoRepository repository;

    private Organizacao org() {
        return Cenario.organizacao(em, "Org");
    }

    @Test
    void persisteELePontosCorridosComConfigECriteriosNaOrdem() {
        Organizacao org = org();
        List<CriterioDesempate> criterios = List.of(CriterioDesempate.CONFRONTO_DIRETO, CriterioDesempate.SALDO,
                CriterioDesempate.PONTOS, CriterioDesempate.SORTEIO);
        Campeonato novo = Campeonato.pontosCorridos(org, "  Liga dos Amigos ", "Futebol society", Cenario.slugUnico(),
                new ConfigPontosCorridosEmbutida(2, 3, 1, 0, false, criterios));
        novo.definirPublico(true);
        novo.registrarSeedSorteio(42L);
        repository.save(novo);
        em.flush();
        em.clear();

        Campeonato lido = repository.findByIdAndOrganizacaoId(novo.getId(), org.getId()).orElseThrow();

        assertThat(lido.getNome()).isEqualTo("Liga dos Amigos");
        assertThat(lido.getFormato()).isEqualTo(FormatoCampeonato.PONTOS_CORRIDOS);
        assertThat(lido.getStatus()).isEqualTo(StatusCampeonato.RASCUNHO);
        assertThat(lido.isPublico()).isTrue();
        assertThat(lido.getSeedSorteio()).isEqualTo(42L);
        assertThat(lido.getCriadoEm()).isNotNull();
        assertThat(lido.getVersion()).isZero();
        assertThat(lido.getConfigMataMata()).isNull();
        ConfigPontosCorridosEmbutida config = lido.getConfigPontosCorridos();
        assertThat(config.getTurnos()).isEqualTo(2);
        assertThat(config.getPontosVitoria()).isEqualTo(3);
        assertThat(config.getPontosEmpate()).isEqualTo(1);
        assertThat(config.getPontosDerrota()).isZero();
        assertThat(config.isPermiteEmpate()).isFalse();
        assertThat(config.getCriteriosDesempate()).containsExactlyElementsOf(criterios);
    }

    @Test
    void persisteELeMataMataComConfigEDesempatesNaOrdem() {
        Organizacao org = org();
        List<DesempateMataMata> desempates = List.of(DesempateMataMata.PENALTIS, DesempateMataMata.GOLS_FORA,
                DesempateMataMata.SORTEIO);
        Campeonato novo = Campeonato.mataMata(org, "Copa", "Xadrez", Cenario.slugUnico(),
                new ConfigMataMataEmbutida(TipoConfronto.SERIE, 5, desempates, Seeding.CABECAS_DE_CHAVE, true));
        repository.save(novo);
        em.flush();
        em.clear();

        Campeonato lido = repository.findByIdAndOrganizacaoId(novo.getId(), org.getId()).orElseThrow();

        assertThat(lido.getFormato()).isEqualTo(FormatoCampeonato.MATA_MATA);
        assertThat(lido.isPublico()).isFalse();
        assertThat(lido.getSeedSorteio()).isNull();
        assertThat(lido.getConfigPontosCorridos()).isNull();
        ConfigMataMataEmbutida config = lido.getConfigMataMata();
        assertThat(config.getTipoConfronto()).isEqualTo(TipoConfronto.SERIE);
        assertThat(config.getMelhorDe()).isEqualTo(5);
        assertThat(config.getSeeding()).isEqualTo(Seeding.CABECAS_DE_CHAVE);
        assertThat(config.isDisputaTerceiroLugar()).isTrue();
        assertThat(config.getDesempates()).containsExactlyElementsOf(desempates);
    }

    @Test
    void mataMataSemSerieGuardaMelhorDeNulo() {
        Organizacao org = org();
        Campeonato novo = Cenario.mataMata(em, org);
        em.flush();
        em.clear();

        Campeonato lido = repository.findByIdAndOrganizacaoId(novo.getId(), org.getId()).orElseThrow();

        assertThat(lido.getConfigMataMata().getMelhorDe()).isNull();
        assertThat(lido.getConfigMataMata().getTipoConfronto()).isEqualTo(TipoConfronto.JOGO_UNICO);
    }

    @Test
    void atualizacaoIncrementaAVersao() {
        Organizacao org = org();
        Campeonato campeonato = Cenario.pontosCorridos(em, org);
        em.flush();

        campeonato.iniciar();
        em.flush();

        assertThat(campeonato.getVersion()).isEqualTo(1);
    }

    @Test
    void slugDuplicadoFalha() {
        Organizacao org = org();
        Campeonato primeiro = Cenario.pontosCorridos(em, org);
        Campeonato duplicado = Campeonato.pontosCorridos(Cenario.organizacao(em, "Outra"), "Outro", "Xadrez",
                primeiro.getSlug(), ConfigPontosCorridosEmbutida.padrao(1));

        assertThatThrownBy(() -> em.persistAndFlush(duplicado))
                .rootCause().hasMessageContaining("uk_campeonato_slug");
    }

    @Test
    void findBySlugEncontraOCampeonato() {
        Campeonato campeonato = Cenario.pontosCorridos(em, org());
        em.flush();
        em.clear();

        assertThat(repository.findBySlug(campeonato.getSlug())).get().extracting(Campeonato::getId)
                .isEqualTo(campeonato.getId());
        assertThat(repository.findBySlug("nao-existe")).isEmpty();
    }

    @Test
    void contaApenasCampeonatosAtivosDaOrganizacao() {
        Organizacao org = org();
        Organizacao outra = org();
        Cenario.pontosCorridos(em, org);
        Campeonato emAndamento = Cenario.pontosCorridos(em, org);
        emAndamento.iniciar();
        Campeonato finalizado = Cenario.mataMata(em, org);
        finalizado.iniciar();
        finalizado.finalizar();
        Cenario.pontosCorridos(em, outra);
        em.flush();

        long ativos = repository.countByOrganizacaoIdAndStatusIn(org.getId(),
                List.of(StatusCampeonato.RASCUNHO, StatusCampeonato.EM_ANDAMENTO));

        assertThat(ativos).isEqualTo(2);
        assertThat(repository.countByOrganizacaoIdAndStatusIn(org.getId(), List.of(StatusCampeonato.FINALIZADO)))
                .isEqualTo(1);
    }
}
