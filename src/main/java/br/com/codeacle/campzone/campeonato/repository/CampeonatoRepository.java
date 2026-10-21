package br.com.codeacle.campzone.campeonato.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.codeacle.campzone.campeonato.domain.Campeonato;
import br.com.codeacle.campzone.campeonato.domain.StatusCampeonato;

/**
 * Acesso a {@link Campeonato} sempre filtrado pela organização (tenant). Não estende {@code JpaRepository} de propósito:
 * assim não existe {@code findAll()} e {@code findById()} sem filtro de organização.
 */
public interface CampeonatoRepository extends Repository<Campeonato, Long> {

    <S extends Campeonato> S save(S entidade);

    Optional<Campeonato> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<Campeonato> findAllByOrganizacaoId(Long organizacaoId);

    /**
     * Busca por slug <b>sem filtro de organização</b>: existe só para a futura página pública. Quem usar deve
     * verificar {@code isPublico()} e nunca expor um campeonato não público. Não use em rotas do organizador.
     */
    Optional<Campeonato> findBySlug(String slug);

    /**
     * Conta os campeonatos da organização nos status informados (ativos: RASCUNHO e EM_ANDAMENTO). O status é
     * gravado como texto (sem CHECK de enum), então a consulta compara pelos nomes e este método converte.
     */
    default long countByOrganizacaoIdAndStatusIn(Long organizacaoId, Collection<StatusCampeonato> status) {
        return contarPorOrganizacaoEStatusTexto(organizacaoId, status.stream().map(Enum::name).toList());
    }

    /** Implementação da contagem por status; prefira {@link #countByOrganizacaoIdAndStatusIn}. */
    @Query("select count(c) from Campeonato c where c.organizacao.id = :organizacaoId and c.status in :status")
    long contarPorOrganizacaoEStatusTexto(@Param("organizacaoId") Long organizacaoId,
            @Param("status") Collection<String> status);
}
