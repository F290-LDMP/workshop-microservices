package se.magnus.api.core.review;

import br.com.fatecararas.api.core.review.Review;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewApplicationTests {
    private static final AtomicInteger IDS = new AtomicInteger(910000);
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void createReadAndDeletePreservePublicIdsAndServerAddress() throws Exception {
        int productId = IDS.incrementAndGet();
        try {
            ResponseEntity<Review> created = create(productId, 77);
            assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(created.getBody().getReviewId()).isEqualTo(77);
            assertThat(created.getBody().getProductId()).isEqualTo(productId);
            assertThat(created.getBody().getServiceAddress()).startsWith("http://").doesNotContain("null");
            Review[] reviews = http.getForObject("/review?productId=" + productId, Review[].class);
            assertThat(reviews).hasSize(1);
            assertThat(reviews[0].getContent()).isEqualTo("Conteúdo de teste");
            ResponseEntity<String> deleted = http.exchange("/review?productId=" + productId,
                    HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
            assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(deleted.getBody()).isNullOrEmpty();
            assertThat(http.getForObject("/review?productId=" + productId, Review[].class)).isEmpty();
            assertThat(http.exchange("/review?productId=" + productId, HttpMethod.DELETE,
                    HttpEntity.EMPTY, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally { http.delete("/review?productId=" + productId); }
    }

    @Test
    void duplicateIs422ButSameReviewIdForDifferentProductsIsAllowed() throws Exception {
        int first = IDS.incrementAndGet(), second = IDS.incrementAndGet();
        try {
            assertThat(create(first, 88).getStatusCode()).isEqualTo(HttpStatus.OK);
            Review body = new Review(first, 88, "Autor", "Assunto", "Conteúdo", null);
            assertError(http.postForEntity("/review", body, String.class), 422);
            assertThat(create(second, 88).getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally {
            http.delete("/review?productId=" + first);
            http.delete("/review?productId=" + second);
        }
    }

    @Test
    void invalidProductIdIs422OnAllOperations() throws Exception {
        assertError(http.getForEntity("/review?productId=0", String.class), 422);
        assertError(http.exchange("/review?productId=-1", HttpMethod.DELETE,
                HttpEntity.EMPTY, String.class), 422);
        assertError(http.postForEntity("/review", new Review(0, 1, "Autor", "Assunto", "Texto", null),
                String.class), 422);
    }

    @Test
    void malformedRequestsAre400() throws Exception {
        assertError(http.getForEntity("/review", String.class), 400);
        assertError(http.getForEntity("/review?productId=abc", String.class), 400);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        assertError(http.postForEntity("/review", new HttpEntity<>("{", headers), String.class), 400);
        assertError(http.postForEntity("/review", new HttpEntity<>("null", headers), String.class), 400);
    }

    @Test
    void invalidFieldsAre422() throws Exception {
        int id = IDS.incrementAndGet();
        assertError(http.postForEntity("/review", new Review(id, 0, "A", "S", "C", null), String.class), 422);
        assertError(http.postForEntity("/review", new Review(id, 1, " ", "S", "C", null), String.class), 422);
        assertError(http.postForEntity("/review", new Review(id, 1, "A", "S", "x".repeat(2001), null), String.class), 422);
    }

    @Test
    void openApiExposesInheritedMappingsAndErrorSchema() throws Exception {
        ResponseEntity<String> response = http.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode doc = mapper.readTree(response.getBody());
        for (String method : new String[]{"get", "post", "delete"}) {
            JsonNode operation = doc.at("/paths/~1review/" + method);
            assertThat(operation.isMissingNode()).isFalse();
            assertThat(operation.at("/responses/200").isMissingNode()).isFalse();
            assertThat(operation.at("/responses/422/content/application~1json/schema/$ref").asText())
                    .isEqualTo("#/components/schemas/HttpErrorInfo");
        }
        JsonNode fields = doc.at("/components/schemas/HttpErrorInfo/properties");
        for (String field : new String[]{"timestamp", "path", "status", "error", "message"}) {
            assertThat(fields.has(field)).isTrue();
        }
    }

    @Test
    void migrationsPreserveSeedReviewsAndAddInternalIdentityAndVersion() {
        assertThat(jdbc.queryForObject("SELECT review_id FROM reviews WHERE id = 1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM reviews WHERE id = 1", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT product_id FROM reviews WHERE id = 1", Integer.class)).isEqualTo(1);
    }

    private ResponseEntity<Review> create(int productId, int reviewId) {
        return http.postForEntity("/review", new Review(productId, reviewId, "Autor", "Assunto",
                "Conteúdo de teste", "http://endereco-do-cliente:1"), Review.class);
    }

    private void assertError(ResponseEntity<String> response, int status) throws Exception {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        JsonNode body = mapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(5);
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("path").asText()).isEqualTo("/review");
        assertThat(body.get("message").asText()).isNotBlank();
        assertThat(body.get("error").asText()).isNotBlank();
        ZonedDateTime.parse(body.get("timestamp").asText());
    }
}
