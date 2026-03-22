package com.mcart.product.config;

import com.mcart.product.security.JsonAccessDeniedHandler;
import com.mcart.product.security.JsonAuthenticationFailureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final JsonAuthenticationFailureHandler authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ObjectProvider<ReactiveJwtDecoder> jwtDecoderProvider) {

        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        if (securityProperties.isEnabled()) {
            if (jwtDecoderProvider.getIfAvailable() == null) {
                throw new IllegalStateException(
                        "app.security.enabled=true requires spring.security.oauth2.resourceserver.jwt.issuer-uri "
                                + "or spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
            }
            if (!CollectionUtils.isEmpty(securityProperties.getCorsAllowedOrigins())) {
                http.cors(c -> c.configurationSource(corsConfigurationSource(securityProperties.getCorsAllowedOrigins())));
            }
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
            http.authorizeExchange(this::authorizeApi);
            http.exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));
        } else {
            if (!CollectionUtils.isEmpty(securityProperties.getCorsAllowedOrigins())) {
                http.cors(c -> c.configurationSource(corsConfigurationSource(securityProperties.getCorsAllowedOrigins())));
            }
            http.authorizeExchange(ex -> ex.anyExchange().permitAll());
        }

        return http.build();
    }

    private void authorizeApi(ServerHttpSecurity.AuthorizeExchangeSpec ex) {
        ex.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();
        ex.pathMatchers("/health").permitAll();
        ex.pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
        // OpenAPI / Swagger UI (springdoc)
        ex.pathMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll();
        ex.pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/swagger-ui/**").permitAll();
        String scope = securityProperties.getRequiredScope();
        if (StringUtils.hasText(scope)) {
            String authority = "SCOPE_" + scope.trim();
            // All /api/** methods require the scope (admin-only product API surface).
            ex.pathMatchers(HttpMethod.GET, "/api/**").hasAuthority(authority);
            ex.pathMatchers(HttpMethod.HEAD, "/api/**").hasAuthority(authority);
            ex.pathMatchers(HttpMethod.POST, "/api/**").hasAuthority(authority);
            ex.pathMatchers(HttpMethod.PUT, "/api/**").hasAuthority(authority);
            ex.pathMatchers(HttpMethod.DELETE, "/api/**").hasAuthority(authority);
            ex.pathMatchers(HttpMethod.PATCH, "/api/**").hasAuthority(authority);
        } else {
            ex.pathMatchers("/api/**").authenticated();
        }
        ex.anyExchange().denyAll();
    }

    private static CorsConfigurationSource corsConfigurationSource(List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
