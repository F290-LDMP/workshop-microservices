package br.com.fatecararas.cloud.config;

import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.fasterxml.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerGitTest {
    static final Path git = createRepository();
    @Autowired TestRestTemplate http;

    static void command(String... args) throws Exception {
        Process process = new ProcessBuilder(args).directory(git.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        if (process.waitFor() != 0) throw new IllegalStateException(output);
    }
    static Path createRepository() {
        try { return Files.createTempDirectory("workshop-config-test-"); }
        catch (IOException e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        Path config = Files.createDirectories(git.resolve("config-repo"));
        Path source = Path.of(System.getProperty("workshop.config.samples"));
        try (var files = Files.list(source)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".yml")).toList())
                Files.copy(file, config.resolve(file.getFileName()));
        }
        Files.writeString(config.resolve("product-local.yml"), Files.readString(config.resolve("product-local.yml")) + "server:\n  port: 17001\n");
        command("git", "init", "-b", "main");
        command("git", "add", ".");
        command("git", "-c", "user.name=Workshop", "-c", "user.email=workshop@example.test", "commit", "-m", "initial");
        command("git", "checkout", "-b", "classroom");
        Files.writeString(config.resolve("product-local.yml"), Files.readString(config.resolve("product-local.yml")).replace("17001", "18001"));
        command("git", "add", ".");
        command("git", "-c", "user.name=Workshop", "-c", "user.email=workshop@example.test", "commit", "-m", "classroom");
        command("git", "checkout", "main");
        registry.add("CONFIG_GIT_URI", () -> git.toUri().toString());
        registry.add("CONFIG_GIT_LABEL", () -> "main");
        registry.add("CONFIG_SERVER_USER", () -> "test");
        registry.add("CONFIG_SERVER_PASSWORD", () -> "test-secret");
    }
    JsonNode environment(String path) {
        var response = http.withBasicAuth("test", "test-secret").getForEntity(path, JsonNode.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().get("version"));
        return response.getBody();
    }
    String property(JsonNode environment, String key) {
        for (JsonNode source : environment.get("propertySources")) {
            JsonNode value = source.get("source").get(key);
            if (value != null) return value.asText();
        }
        throw new AssertionError("Missing property: " + key);
    }
    @Test void protectsConfigurationAndKeepsHealthPublic() {
        assertEquals(401, http.getForEntity("/product/local/main", String.class).getStatusCode().value());
        assertEquals(401, http.withBasicAuth("test", "wrong").getForEntity("/product/local/main", String.class).getStatusCode().value());
        assertEquals(200, http.getForEntity("/actuator/health", String.class).getStatusCode().value());
    }
    @Test void resolvesApplicationProfileAndGitLabel() {
        assertEquals("17001", property(environment("/product/local/main"), "server.port"));
        assertEquals("18001", property(environment("/product/local/classroom"), "server.port"));
        assertEquals("central-configuration", property(environment("/product/docker/main"), "info.workshop.lesson"));
        assertEquals("rabbitmq", property(environment("/product/docker/main"), "spring.rabbitmq.host"));
        assertEquals("http://keycloak:8080/realms/workshop/protocol/openid-connect/certs",
            property(environment("/gateway/docker/main"), "spring.security.oauth2.resourceserver.jwt.jwk-set-uri"));
        for (String app : new String[]{"product", "recommendation", "review", "product-composite", "gateway", "eureka-server"}) {
            assertEquals(app, property(environment("/" + app + "/docker/main"), "info.workshop.application"));
            assertEquals("local", property(environment("/" + app + "/local/main"), "info.workshop.profile"));
        }
        assertEquals(404, http.withBasicAuth("test", "test-secret").getForEntity("/product/local/missing-label", String.class).getStatusCode().value());
    }
}
