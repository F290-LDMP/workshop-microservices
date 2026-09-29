package br.com.fatecararas.microservices.core.product;

import br.com.fatecararas.microservices.core.product.persistence.ProductEntity;
import br.com.fatecararas.microservices.core.product.persistence.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.endsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class ProductServiceTests {
    @Autowired MockMvc mvc;
    @MockBean ProductRepository repository;
    @LocalServerPort int port;

    @Test
    void createsProductAndReplacesClientAddress() throws Exception {
        when(repository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/product").contentType("application/json")
                .content("{\"productId\":1,\"name\":\"Produto\",\"weight\":100,\"serviceAddress\":\"fake\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("Produto"))
                .andExpect(jsonPath("$.weight").value(100))
                .andExpect(jsonPath("$.serviceAddress", endsWith(":" + port)));
    }

    @Test
    void getsPersistedProduct() throws Exception {
        when(repository.findByProductId(1)).thenReturn(Optional.of(new ProductEntity(1, "Produto", 100)));
        mvc.perform(get("/product/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("Produto"))
                .andExpect(jsonPath("$.weight").value(100))
                .andExpect(jsonPath("$.serviceAddress", endsWith(":" + port)));
    }

    @Test
    void missingProductReturnsContractError() throws Exception {
        when(repository.findByProductId(13)).thenReturn(Optional.empty());
        mvc.perform(get("/product/13")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/product/13"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No product found for productId: 13"));
    }

    @Test
    void rejectsInvalidIdsBeforeAccessingDatabase() throws Exception {
        for (int id : new int[]{0, -1}) {
            mvc.perform(get("/product/" + id)).andExpect(status().isUnprocessableEntity());
            mvc.perform(delete("/product/" + id)).andExpect(status().isUnprocessableEntity());
            mvc.perform(post("/product").contentType("application/json")
                    .content("{\"productId\":" + id + ",\"name\":\"Produto\",\"weight\":100}"))
                    .andExpect(status().isUnprocessableEntity());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void duplicateProductReturnsContractError() throws Exception {
        when(repository.save(any(ProductEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));
        mvc.perform(post("/product").contentType("application/json")
                .content("{\"productId\":1,\"name\":\"Produto\",\"weight\":100}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/product"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Duplicate productId: 1"));
    }

    @Test
    void deletesExistingProduct() throws Exception {
        ProductEntity entity = new ProductEntity(1, "Produto", 100);
        when(repository.findByProductId(1)).thenReturn(Optional.of(entity));
        mvc.perform(delete("/product/1")).andExpect(status().isOk()).andExpect(content().string(""));
        verify(repository).delete(entity);
    }

    @Test
    void deletingAbsentProductIsIdempotent() throws Exception {
        when(repository.findByProductId(1)).thenReturn(Optional.empty());
        mvc.perform(delete("/product/1")).andExpect(status().isOk());
        verify(repository, never()).delete(any(ProductEntity.class));
    }
}
