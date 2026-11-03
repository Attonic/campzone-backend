package br.com.codeacle.campzone.admin.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;

/**
 * As consultas <b>entre organizações</b> do admin da plataforma (ADR 0002). É o único lugar do sistema que lê dados
 * de todas as organizações: nenhuma consulta daqui filtra por tenant, de propósito, e por isso só é usado pelo
 * {@code AdminPlataformaService}, atrás de {@code /api/v1/admin/**}. Não reutilize em rotas do organizador.
 * <p>
 * O status do campeonato é gravado como texto (sem CHECK de enum, ADR 0003), então os status chegam como nomes.
 * A busca por nome usa {@code !} como caractere de escape do {@code like}.
 */
public interface AdminConsultaRepository extends Repository<Organizacao, Long> {

    /** Caractere de escape do {@code like} das consultas por nome. */
    char ESCAPE_LIKE = '!';

    @Query("select count(o) from Organizacao o")
    long contarOrganizacoes();

    @Query("select count(u) from Usuario u")
    long contarUsuarios();

    @Query("select count(c) from Campeonato c")
    long contarCampeonatos();

    @Query("select count(c) from Campeonato c where c.status in :status")
    long contarCampeonatosPorStatus(@Param("status") Collection<String> status);

    @Query("""
            select count(o) from Organizacao o
            where lower(o.nome) like lower(:padrao) escape '!'
            """)
    long contarOrganizacoesPorNome(@Param("padrao") String padrao);

    /**
     * Uma página de organizações com plano e contagens, mais novas primeiro (desempate por id decrescente). As
     * contagens são subconsultas correlacionadas na mesma consulta: o custo não depende do número de organizações da
     * página. {@code padrao} é um padrão de {@code like} (use {@code "%"} para todas) e {@code statusAtivos} são os
     * status que contam como campeonato ativo.
     */
    @Query("""
            select new br.com.codeacle.campzone.admin.repository.OrganizacaoComContagens(
                o.id, o.nome, p.codigo, p.nome,
                (select count(u) from Usuario u where u.organizacao.id = o.id),
                (select count(c) from Campeonato c where c.organizacao.id = o.id),
                (select count(a) from Campeonato a where a.organizacao.id = o.id and a.status in :statusAtivos),
                o.criadoEm)
            from Organizacao o join o.plano p
            where lower(o.nome) like lower(:padrao) escape '!'
            order by o.criadoEm desc, o.id desc
            """)
    List<OrganizacaoComContagens> listarOrganizacoes(@Param("padrao") String padrao,
            @Param("statusAtivos") Collection<String> statusAtivos, Pageable pageable);
}
