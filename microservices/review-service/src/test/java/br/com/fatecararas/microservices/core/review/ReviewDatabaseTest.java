package br.com.fatecararas.microservices.core.review;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "eureka.client.enabled=false", "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
class ReviewDatabaseTest {
    @Container static MySQLContainer<?> database = new MySQLContainer<>("mysql:8.0.32");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword); }
    @Autowired MockMvc mvc;
    @Test void persistsRejectsDuplicateAndDeletesIdempotently() throws Exception {
        String body = """
            {"productId":991,"reviewId":1,"author":"Ana","subject":"Teste","content":"OK"}
            """;
        mvc.perform(post("/review").contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(post("/review").contentType("application/json").content(body)).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/review?productId=991")).andExpect(status().isOk()).andExpect(jsonPath("$[0].subject").value("Teste"));
        mvc.perform(delete("/review?productId=991")).andExpect(status().isOk());
        mvc.perform(delete("/review?productId=991")).andExpect(status().isOk());
        mvc.perform(get("/review?productId=991")).andExpect(content().json("[]"));
        mvc.perform(get("/review?productId=0")).andExpect(status().isUnprocessableEntity());
    }
}
