package br.com.codeacle.campzone.common.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import br.com.codeacle.campzone.common.security.JwtProperties;

/**
 * Falha rápida e com mensagem clara no perfil {@code prod} quando faltam as variáveis obrigatórias: banco
 * ({@code DB_*}), segredo do JWT ({@code JWT_SECRET}, mínimo 32 bytes) e credenciais do admin da plataforma
 * ({@code ADMIN_EMAIL}, {@code ADMIN_SENHA}). Roda antes da criação de qualquer bean, evitando erros obscuros
 * (pool de conexões com a URL não resolvida, placeholder sem valor). A checagem do banco exige {@code DB_URL},
 * {@code DB_USER} e {@code DB_PASSWORD} de propósito: {@code SPRING_DATASOURCE_*} sozinhas não bastam, porque o
 * {@code application-prod.yml} só lê as variáveis {@code DB_*}.
 */
@Component
@Profile("prod")
public class VariaveisObrigatoriasProd implements BeanFactoryPostProcessor, EnvironmentAware {

    static final List<String> OBRIGATORIAS = List.of("DB_URL", "DB_USER", "DB_PASSWORD", "JWT_SECRET",
            "ADMIN_EMAIL", "ADMIN_SENHA");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        List<String> ausentes = ausentes(environment);
        if (!ausentes.isEmpty()) {
            throw new IllegalStateException("Perfil prod: variáveis de ambiente obrigatórias ausentes ou vazias: "
                    + String.join(", ", ausentes) + ". Defina-as antes de iniciar a aplicação.");
        }
        String segredo = environment.getProperty("JWT_SECRET");
        if (segredo.getBytes(StandardCharsets.UTF_8).length < JwtProperties.TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalStateException("Perfil prod: JWT_SECRET deve ter no mínimo "
                    + JwtProperties.TAMANHO_MINIMO_SEGREDO_BYTES + " bytes.");
        }
    }

    static List<String> ausentes(Environment environment) {
        return OBRIGATORIAS.stream()
                .filter(nome -> {
                    String valor = environment.getProperty(nome);
                    return valor == null || valor.isBlank();
                })
                .toList();
    }
}
