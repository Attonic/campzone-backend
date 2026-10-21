package br.com.codeacle.campzone.common.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Segurança mínima do scaffold: API stateless, rotas públicas em {@code /api/v1/public/**} e o resto exige
 * autenticação. A autenticação por JWT entra na tarefa 02.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // O dispatch de erro do container (/error) não pode virar 401 em rota pública.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((request, response, ex) -> escreverProblema(response, jsonMapper,
                                HttpStatus.UNAUTHORIZED, "Não autenticado", "Autenticação necessária."))
                        .accessDeniedHandler((request, response, ex) -> escreverProblema(response, jsonMapper,
                                HttpStatus.FORBIDDEN, "Acesso negado", "Sem permissão para este recurso.")));
        return http.build();
    }

    /**
     * Impede o Spring Boot de criar o usuário padrão com senha gerada no log. Nenhum usuário existe até a
     * tarefa 02 (auth), que substitui este bean.
     */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Autenticação ainda não implementada");
        };
    }

    private static void escreverProblema(HttpServletResponse response, JsonMapper jsonMapper, HttpStatus status,
            String titulo, String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(problema));
    }
}
