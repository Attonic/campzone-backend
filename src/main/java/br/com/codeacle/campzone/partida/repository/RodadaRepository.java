package br.com.codeacle.campzone.partida.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import br.com.codeacle.campzone.partida.domain.Rodada;

/**
 * Acesso a {@link Rodada} sempre filtrado pela organização (tenant). Não estende {@code JpaRepository} de propósito:
 * assim não existe {@code findAll()} e {@code findById()} sem filtro de organização.
 */
public interface RodadaRepository extends Repository<Rodada, Long> {

    <S extends Rodada> S save(S entidade);

    Optional<Rodada> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<Rodada> findAllByOrganizacaoId(Long organizacaoId);

    List<Rodada> findAllByFaseIdAndOrganizacaoIdOrderByNumeroAsc(Long faseId, Long organizacaoId);
}
