package br.com.codeacle.campzone.partida.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import br.com.codeacle.campzone.partida.domain.Partida;

/**
 * Acesso a {@link Partida} sempre filtrado pela organização (tenant). Não estende {@code JpaRepository} de propósito:
 * assim não existe {@code findAll()} e {@code findById()} sem filtro de organização.
 */
public interface PartidaRepository extends Repository<Partida, Long> {

    <S extends Partida> S save(S entidade);

    Optional<Partida> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<Partida> findAllByOrganizacaoId(Long organizacaoId);

    List<Partida> findAllByFaseIdAndOrganizacaoId(Long faseId, Long organizacaoId);

    List<Partida> findAllByConfrontoIdAndOrganizacaoIdOrderByNumeroJogoAsc(Long confrontoId, Long organizacaoId);
}
