package br.com.codeacle.campzone.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import br.com.codeacle.campzone.TestcontainersConfiguration;
import br.com.codeacle.testes.ApiTesteController;

/**
 * Teste de integração: aplicação inteira (perfil dev, seeders incluídos) contra PostgreSQL real e MockMvc com a
 * cadeia de filtros de segurança. Importa o {@link ApiTesteController}, que fica fora do component scan. Usar
 * sempre esta anotação mantém um único contexto (e um único container) entre as classes de teste.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ApiTesteController.class })
@ActiveProfiles("dev")
public @interface IntegracaoTest {
}
