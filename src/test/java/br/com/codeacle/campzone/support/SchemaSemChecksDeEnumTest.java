package br.com.codeacle.campzone.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Com {@code ddl-auto=update} o Hibernate não atualiza CHECKs já criadas: uma CHECK de valores de enum travaria a
 * evolução do enum em bancos existentes. Os enums são gravados por conversores (sem CHECK); este teste garante que
 * nenhuma CHECK implícita volte e que só existam as CHECKs explícitas (nome começando por {@code ck_}).
 */
@RepositorioTest
class SchemaSemChecksDeEnumTest {

    @Autowired
    private TestEntityManager em;

    @SuppressWarnings("unchecked")
    @Test
    void apenasChecksExplicitasExistemNoSchema() {
        List<Object[]> checks = em.getEntityManager().createNativeQuery(
                "select conrelid::regclass::text, conname from pg_constraint "
                        + "where contype = 'c' and connamespace = 'public'::regnamespace order by 1, 2")
                .getResultList();

        List<String> implicitas = checks.stream()
                .filter(linha -> !((String) linha[1]).startsWith("ck_"))
                // CHECK de coluna @OrderColumn (posicao >= 0): não depende de enum e nunca precisa evoluir
                .filter(linha -> !((String) linha[1]).endsWith("_posicao_check"))
                .map(linha -> linha[0] + "." + linha[1])
                .toList();

        assertThat(implicitas).as("CHECKs geradas pelo Hibernate (ex.: de enum)").isEmpty();
        assertThat(checks).extracting(linha -> (String) linha[1])
                .contains("ck_resultado_placares", "ck_partida_rodada_xor_confronto");
    }
}
