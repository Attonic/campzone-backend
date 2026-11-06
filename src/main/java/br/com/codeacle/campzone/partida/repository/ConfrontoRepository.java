package br.com.codeacle.campzone.partida.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import br.com.codeacle.campzone.partida.domain.Confronto;

/**
 * Acesso a {@link Confronto} sempre filtrado pela organização (tenant). Não estende {@code JpaRepository} de propósito:
 * assim não existe {@code findAll()} e {@code findById()} sem filtro de organização.
 */
public interface ConfrontoRepository extends Repository<Confronto, Long> {

    <S extends Confronto> S save(S entidade);

    Optional<Confronto> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<Confronto> findAllByOrganizacaoId(Long organizacaoId);

    List<Confronto> findAllByFaseIdAndOrganizacaoIdOrderByPosicaoAsc(Long faseId, Long organizacaoId);
}
