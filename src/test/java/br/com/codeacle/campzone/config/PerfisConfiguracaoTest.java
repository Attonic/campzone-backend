package br.com.codeacle.campzone.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import br.com.codeacle.campzone.CampzoneApplication;

/**
 * Verifica os perfis de configuração sem subir banco: os contextos usam uma configuração vazia (sem
 * auto-configuração), então só o carregamento dos arquivos application*.yml é exercitado.
 */
class PerfisConfiguracaoTest {

    private static final String SEGREDO = "segredo-de-teste-campzone-0123456789-abcdef";

    @Configuration
    static class ConfiguracaoVazia {
    }

    /**
     * Monta a aplicação isolada do ambiente de quem roda: remove variáveis de ambiente e system properties
     * (DB_URL, SPRING_PROFILES_ACTIVE...) e injeta as propriedades do teste com a maior precedência.
     */
    private SpringApplicationBuilder construtor(Class<?> fonte, String... propriedades) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        Map<String, Object> mapa = new HashMap<>();
        for (String propriedade : propriedades) {
            int separador = propriedade.indexOf('=');
            mapa.put(propriedade.substring(0, separador), propriedade.substring(separador + 1));
        }
        env.getPropertySources().addFirst(new MapPropertySource("propriedadesDoTeste", mapa));
        return new SpringApplicationBuilder(fonte)
                .web(WebApplicationType.NONE)
                .environment(env);
    }

    private SpringApplicationBuilder construtor(String... propriedades) {
        return construtor(ConfiguracaoVazia.class, propriedades);
    }

    @Test
    void semPerfilInformadoOPadraoEODev() {
        try (ConfigurableApplicationContext contexto = construtor().run()) {
            Environment env = contexto.getEnvironment();
            assertThat(env.getDefaultProfiles()).contains("dev");
            assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5433/campzone");
            assertThat(env.getProperty("logging.level.br.com.codeacle.campzone")).isEqualTo("DEBUG");
            assertThat(env.getProperty("campzone.admin.email")).isEqualTo("admin@campzone.dev");
            assertThat(env.getProperty("campzone.jwt.segredo")).hasSizeGreaterThanOrEqualTo(32);
        }
    }

    @Test
    void prodComVariaveisUsaValoresDeProducao() {
        try (ConfigurableApplicationContext contexto = construtor(
                "spring.profiles.active=prod",
                "DB_URL=jdbc:postgresql://db:5432/x", "DB_USER=u", "DB_PASSWORD=p").run()) {
            Environment env = contexto.getEnvironment();
            assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://db:5432/x");
            assertThat(env.getProperty("server.port")).isEqualTo("8080");
            assertThat(env.getProperty("server.error.include-stacktrace")).isEqualTo("never");
            assertThat(env.getProperty("server.error.include-message")).isEqualTo("never");
            assertThat(env.getProperty("spring.jpa.show-sql")).isEqualTo("false");
            assertThat(env.getProperty("logging.level.br.com.codeacle.campzone")).isEqualTo("INFO");
            assertThat(env.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("10");
        }
    }

    @Test
    void prodNaoTemDefaultParaCredenciaisDoBanco() {
        try (ConfigurableApplicationContext contexto = construtor("spring.profiles.active=prod").run()) {
            Environment env = contexto.getEnvironment();
            for (String chave : new String[] { "spring.datasource.url", "spring.datasource.username",
                    "spring.datasource.password" }) {
                assertThatThrownBy(() -> env.getProperty(chave))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("DB_");
            }
        }
    }

    @Test
    void aplicacaoRealEmProdSemVariaveisDoBancoNaoSobe() {
        assertThatThrownBy(() -> construtor(CampzoneApplication.class, "spring.profiles.active=prod").run())
                .hasStackTraceContaining("variáveis de ambiente obrigatórias ausentes")
                .hasStackTraceContaining("DB_URL, DB_USER, DB_PASSWORD");
    }

    @Test
    void aplicacaoRealEmProdComDbUrlVazioCitaApenasDbUrl() {
        assertThatThrownBy(() -> construtor(CampzoneApplication.class, "spring.profiles.active=prod",
                "DB_URL=", "DB_USER=u", "DB_PASSWORD=p", "JWT_SECRET=" + SEGREDO, "ADMIN_EMAIL=a@x.com",
                "ADMIN_SENHA=senha-admin").run())
                .hasStackTraceContaining("ausentes ou vazias: DB_URL. Defina");
    }

    @Test
    void prodSemJwtSecretEAdminNaoSobeEListaOQueFalta() {
        assertThatThrownBy(() -> construtor(CampzoneApplication.class, "spring.profiles.active=prod",
                "DB_URL=jdbc:postgresql://db:5432/x", "DB_USER=u", "DB_PASSWORD=p").run())
                .hasStackTraceContaining("ausentes ou vazias: JWT_SECRET, ADMIN_EMAIL, ADMIN_SENHA. Defina");
    }

    @Test
    void prodSemApenasOJwtSecretCitaSoEle() {
        assertThatThrownBy(() -> construtor(CampzoneApplication.class, "spring.profiles.active=prod",
                "DB_URL=jdbc:postgresql://db:5432/x", "DB_USER=u", "DB_PASSWORD=p", "ADMIN_EMAIL=a@x.com",
                "ADMIN_SENHA=senha-admin").run())
                .hasStackTraceContaining("ausentes ou vazias: JWT_SECRET. Defina");
    }

    @Test
    void prodComJwtSecretCurtoNaoSobe() {
        assertThatThrownBy(() -> construtor(CampzoneApplication.class, "spring.profiles.active=prod",
                "DB_URL=jdbc:postgresql://db:5432/x", "DB_USER=u", "DB_PASSWORD=p", "JWT_SECRET=curto",
                "ADMIN_EMAIL=a@x.com", "ADMIN_SENHA=senha-admin").run())
                .hasStackTraceContaining("JWT_SECRET deve ter no mínimo 32 bytes");
    }

    @Test
    void prodNaoTemDefaultParaSegredoENemParaAdmin() {
        try (ConfigurableApplicationContext contexto = construtor("spring.profiles.active=prod").run()) {
            Environment env = contexto.getEnvironment();
            assertThatThrownBy(() -> env.getProperty("campzone.jwt.segredo"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("JWT_SECRET");
            assertThatThrownBy(() -> env.getProperty("campzone.admin.email"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ADMIN_EMAIL");
            assertThatThrownBy(() -> env.getProperty("campzone.admin.senha"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ADMIN_SENHA");
        }
    }

    @Test
    void aplicacaoRealComProdEOutroPerfilSemVariaveisNaoSobe() {
        assertThatThrownBy(() -> construtor(CampzoneApplication.class, "spring.profiles.active=prod,algo").run())
                .hasStackTraceContaining("DB_URL, DB_USER, DB_PASSWORD");
    }
}
