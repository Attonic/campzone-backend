package br.com.codeacle.campzone.common.config;

import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Falha rápida e com mensagem clara no perfil {@code prod} quando faltam as variáveis do banco. Roda antes da
 * criação de qualquer bean, evitando o erro obscuro do pool de conexões com a URL não resolvida.
 */
@Component
@Profile("prod")
public class VariaveisObrigatoriasProd implements BeanFactoryPostProcessor, EnvironmentAware {

    static final List<String> OBRIGATORIAS = List.of("DB_URL", "DB_USER", "DB_PASSWORD");

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
