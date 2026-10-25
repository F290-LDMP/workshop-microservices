package br.com.fatecararas.cloud.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = GatewaySecurityTest.Probe.class, properties = "spring.cloud.gateway.enabled=false")
@Import({SecurityConfiguration.class, GatewaySecurityTest.Probe.class})
class GatewaySecurityTest {
    @Autowired WebTestClient client;
    @MockBean ReactiveJwtDecoder decoder;
    @RestController static class Probe {
        @GetMapping("/product-composite/1") String read() { return "ok"; }
        @PostMapping("/product-composite") void write() { }
        @DeleteMapping("/product-composite/1") void delete() { }
        @GetMapping("/actuator/health") String health() { return "UP"; }
    }
    @Test void anonymousHealthAndUnauthorizedApi() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
        client.get().uri("/product-composite/1").exchange().expectStatus().isUnauthorized();
    }
    @Test void readScopeCannotWriteOrDelete() {
        var reader = client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("SCOPE_product:read")));
        reader.get().uri("/product-composite/1").exchange().expectStatus().isOk();
        reader.post().uri("/product-composite").exchange().expectStatus().isForbidden();
        reader.delete().uri("/product-composite/1").exchange().expectStatus().isForbidden();
    }
    @Test void writeScopeAndDenyByDefault() {
        var writer = client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("SCOPE_product:write")));
        writer.post().uri("/product-composite").exchange().expectStatus().isOk();
        writer.delete().uri("/product-composite/1").exchange().expectStatus().isOk();
        writer.get().uri("/actuator/env").exchange().expectStatus().isForbidden();
    }
}
