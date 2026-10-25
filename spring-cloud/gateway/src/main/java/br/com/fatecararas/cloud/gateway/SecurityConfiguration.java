package br.com.fatecararas.cloud.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf().disable()
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange()
            .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/product-composite", "/product-composite/**").hasAuthority("SCOPE_product:read")
            .pathMatchers(HttpMethod.POST, "/product-composite", "/product-composite/**").hasAuthority("SCOPE_product:write")
            .pathMatchers(HttpMethod.DELETE, "/product-composite", "/product-composite/**").hasAuthority("SCOPE_product:write")
            .anyExchange().denyAll()
            .and().oauth2ResourceServer().jwt().and().and().build();
    }
}
