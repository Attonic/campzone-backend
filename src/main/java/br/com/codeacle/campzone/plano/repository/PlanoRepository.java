package br.com.codeacle.campzone.plano.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.codeacle.campzone.plano.domain.Plano;

/** Plano é global (sem organização), por isso pode expor as operações padrão do Spring Data. */
public interface PlanoRepository extends JpaRepository<Plano, Long> {

    Optional<Plano> findByCodigo(String codigo);
}
