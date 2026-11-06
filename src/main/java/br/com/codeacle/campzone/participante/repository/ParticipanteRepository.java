package br.com.codeacle.campzone.participante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import br.com.codeacle.campzone.participante.domain.Participante;

/**
 * Acesso a {@link Participante} sempre filtrado pela organização (tenant). Não estende {@code JpaRepository} de propósito:
 * assim não existe {@code findAll()} e {@code findById()} sem filtro de organização.
 */
public interface ParticipanteRepository extends Repository<Participante, Long> {

    <S extends Participante> S save(S entidade);

    Optional<Participante> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<Participante> findAllByOrganizacaoId(Long organizacaoId);

    List<Participante> findAllByCampeonatoIdAndOrganizacaoId(Long campeonatoId, Long organizacaoId);
}
