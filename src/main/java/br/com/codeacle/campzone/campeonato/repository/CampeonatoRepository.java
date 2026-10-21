package br.com.codeacle.campzone.campeonato.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

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

    /** Conta os campeonatos da organização nos status informados (campeonatos ativos: RASCUNHO e EM_ANDAMENTO). */
    long countByOrganizacaoIdAndStatusIn(Long organizacaoId, Collection<StatusCampeonato> status);
}
