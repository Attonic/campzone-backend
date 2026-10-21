package br.com.codeacle.campzone.common.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Falha rápida quando nenhum perfil está ativo (ADR 0004) ou quando {@code dev} e {@code prod} estão ativos juntos. Não existe perfil padrão de propósito: o {@code dev} traz
 * segredo JWT e admin com senha públicos, então um deploy que esquecesse {@code SPRING_PROFILES_ACTIVE=prod}
 * subiria inseguro. Roda antes da criação de qualquer bean.
 */
@Component
public class PerfilObrigatorio implements BeanFactoryPostProcessor, EnvironmentAware {

    static final String MENSAGEM = "Nenhum perfil ativo: defina SPRING_PROFILES_ACTIVE=dev (desenvolvimento) ou prod.";

    static final String MENSAGEM_DEV_E_PROD = "Perfis dev e prod não podem estar ativos ao mesmo tempo.";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        if (environment.getActiveProfiles().length == 0) {
            throw new IllegalStateException(MENSAGEM);
        }
        // dev + prod criaria as contas públicas de dev (DevDadosSeeder) no banco de produção.
        if (environment.acceptsProfiles(Profiles.of("dev & prod"))) {
            throw new IllegalStateException(MENSAGEM_DEV_E_PROD);
        }
    }
}
