package br.com.fatecararas.cloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain configSecurity(HttpSecurity http) throws Exception {
        return http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeHttpRequests()
            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
            .anyRequest().authenticated().and().httpBasic().and().build();
    }
}
