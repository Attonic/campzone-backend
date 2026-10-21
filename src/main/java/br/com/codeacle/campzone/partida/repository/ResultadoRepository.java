package br.com.codeacle.campzone.partida.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import br.com.codeacle.campzone.partida.domain.Resultado;

/**
 * Acesso a {@link Resultado} sempre filtrado pela organização (tenant). Não estende {@code JpaRepository} de propósito:
 * assim não existe {@code findAll()} e {@code findById()} sem filtro de organização.
 */
public interface ResultadoRepository extends Repository<Resultado, Long> {

    <S extends Resultado> S save(S entidade);

    Optional<Resultado> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<Resultado> findAllByOrganizacaoId(Long organizacaoId);

    Optional<Resultado> findByPartidaIdAndOrganizacaoId(Long partidaId, Long organizacaoId);
}
