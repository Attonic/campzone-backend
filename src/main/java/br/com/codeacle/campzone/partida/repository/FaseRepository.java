package br.com.codeacle.campzone.partida.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import br.com.codeacle.campzone.partida.domain.Fase;

/**
 * Acesso a {@link Fase} sempre filtrado pela organização (tenant). Não estende {@code JpaRepository} de propósito:
 * assim não existe {@code findAll()} e {@code findById()} sem filtro de organização.
 */
public interface FaseRepository extends Repository<Fase, Long> {

    <S extends Fase> S save(S entidade);

    Optional<Fase> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<Fase> findAllByOrganizacaoId(Long organizacaoId);

    List<Fase> findAllByCampeonatoIdAndOrganizacaoIdOrderByOrdemAsc(Long campeonatoId, Long organizacaoId);
}
