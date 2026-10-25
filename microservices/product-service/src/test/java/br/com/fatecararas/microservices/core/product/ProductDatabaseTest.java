package br.com.fatecararas.microservices.core.product;
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
class ProductDatabaseTest {
    @Container static MongoDBContainer database = new MongoDBContainer("mongo:6.0.4");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("spring.data.mongodb.uri", database::getReplicaSetUrl); }
    @Autowired MockMvc mvc;
    @Test void persistsRejectsDuplicateAndDeletesIdempotently() throws Exception {
        String body = """
            {"productId":991,"name":"Teste","weight":10}
            """;
        mvc.perform(post("/product").contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(post("/product").contentType("application/json").content(body)).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/product/991")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Teste"));
        mvc.perform(delete("/product/991")).andExpect(status().isOk());
        mvc.perform(delete("/product/991")).andExpect(status().isOk());
        mvc.perform(get("/product/991")).andExpect(status().isNotFound());
        mvc.perform(get("/product/0")).andExpect(status().isUnprocessableEntity());
    }
}
