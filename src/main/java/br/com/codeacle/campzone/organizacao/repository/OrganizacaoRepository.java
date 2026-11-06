package br.com.codeacle.campzone.organizacao.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;

/**
 * A organização é o próprio tenant. Não expõe {@code findAll()}: a listagem entre organizações pertence ao módulo
 * de admin da plataforma (tarefa 11), que a declara de forma explícita.
 */
public interface OrganizacaoRepository extends Repository<Organizacao, Long> {

    <S extends Organizacao> S save(S organizacao);

    Optional<Organizacao> findById(Long id);
}
