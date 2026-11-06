package br.com.codeacle.campzone.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio injetável, para que emissão e expiração de tokens sejam testáveis sem depender da hora real. */
@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
