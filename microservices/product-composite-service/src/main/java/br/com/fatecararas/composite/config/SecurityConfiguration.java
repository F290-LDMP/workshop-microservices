package br.com.fatecararas.composite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    org.springdoc.core.customizers.OpenApiCustomizer bearerSecurity() {
        return openApi -> {
            openApi.getComponents().addSecuritySchemes("bearerAuth",
                new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                    .scheme("bearer").bearerFormat("JWT"));
            openApi.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"));
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeHttpRequests()
            .requestMatchers("/actuator/health", "/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/product-composite", "/product-composite/**").hasAuthority("SCOPE_product:read")
            .requestMatchers(HttpMethod.POST, "/product-composite", "/product-composite/**").hasAuthority("SCOPE_product:write")
            .requestMatchers(HttpMethod.DELETE, "/product-composite", "/product-composite/**").hasAuthority("SCOPE_product:write")
            .anyRequest().denyAll()
            .and().oauth2ResourceServer().jwt().and().and().build();
    }
}
