package br.com.codeacle.campzone.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import br.com.codeacle.campzone.auth.domain.Usuario;

/**
 * O usuário é identificado globalmente por e-mail (login) e por id vindo do {@code sub} do JWT já validado; por isso
 * as buscas não recebem organização. Não expõe {@code findAll()}. Os e-mails são sempre consultados já normalizados
 * ({@link Usuario#normalizarEmail(String)}).
 */
public interface UsuarioRepository extends Repository<Usuario, Long> {

    <S extends Usuario> S saveAndFlush(S usuario);

    @EntityGraph(attributePaths = "organizacao")
    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = "organizacao")
    Optional<Usuario> findById(Long id);

    boolean existsByEmail(String email);
}
