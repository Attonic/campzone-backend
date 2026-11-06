package br.com.codeacle.campzone.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import jakarta.servlet.DispatcherType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Segurança da API (ADR 0002): stateless, JWT como resource server e autorização por papel.
 * <ul>
 *   <li>{@code /api/v1/public/**}: liberado;</li>
 *   <li>{@code /api/v1/admin/**}: só {@code ADMIN_PLATAFORMA};</li>
 *   <li>{@code /api/v1/auth/me}: qualquer autenticado;</li>
 *   <li>demais {@code /api/v1/**}: só {@code ORGANIZADOR};</li>
 *   <li>qualquer outra rota: negada.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final String ROTAS_PUBLICAS = "/api/v1/public/**";

    // Mesmos nomes de auth.domain.Papel (o common não depende do módulo auth); um teste garante a igualdade.
    static final String PAPEL_ADMIN = "ADMIN_PLATAFORMA";
    static final String PAPEL_ORGANIZADOR = "ORGANIZADOR";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) {
        ProblemaSegurancaHandler problemas = new ProblemaSegurancaHandler(jsonMapper);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // O dispatch de erro do container (/error) não pode virar 401 em rota pública.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole(PAPEL_ADMIN)
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/**").hasRole(PAPEL_ORGANIZADOR)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor
                        .bearerTokenResolver(resolverTokenIgnorandoRotasPublicas())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeJwt()))
                        .authenticationEntryPoint(problemas)
                        .accessDeniedHandler(problemas))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(problemas)
                        .accessDeniedHandler(problemas));
        return http.build();
    }

    /** Senhas em bcrypt, com o prefixo do algoritmo ({@code {bcrypt}}) que permite trocar de algoritmo depois. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Firewall do Spring Security barra a requisição antes do DispatcherServlet: devolve ProblemDetail 400. */
    @Bean
    RequestRejectedHandler requestRejectedHandler(JsonMapper jsonMapper) {
        return new ProblemaSegurancaHandler(jsonMapper);
    }

    /** Autoridade {@code ROLE_<papel>} a partir do claim {@code papel}. */
    private static JwtAuthenticationConverter conversorDeJwt() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(TokenClaims.PAPEL);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    /**
     * Rotas públicas não leem o header Authorization: um token velho guardado no navegador não pode transformar o
     * login ou o cadastro em 401.
     */
    private static BearerTokenResolver resolverTokenIgnorandoRotasPublicas() {
        DefaultBearerTokenResolver padrao = new DefaultBearerTokenResolver();
        var rotaPublica = PathPatternRequestMatcher.withDefaults().matcher(ROTAS_PUBLICAS);
        return request -> rotaPublica.matches(request) ? null : padrao.resolve(request);
    }
}
