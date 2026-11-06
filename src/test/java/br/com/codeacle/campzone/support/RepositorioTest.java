package br.com.codeacle.campzone.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import br.com.codeacle.campzone.TestcontainersConfiguration;

/**
 * Teste de repositório contra o PostgreSQL real (Testcontainers), com o schema gerado pelo Hibernate. Usar sempre
 * esta mesma anotação mantém uma única configuração, então o contexto (e o container) é reaproveitado entre as
 * classes de teste. Cada teste roda em uma transação com rollback.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
public @interface RepositorioTest {
}
