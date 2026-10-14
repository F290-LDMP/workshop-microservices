package se.magnus.microservices.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.exception.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

class RecommendationServiceTest {

    @Mock
    private RecommendationRepository repository;
    @Mock
    private ServiceUtil serviceUtil;

    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(serviceUtil.getServiceAddress()).thenReturn("127.0.0.1:7002");
        service = new RecommendationServiceImpl(repository, serviceUtil);
    }

    @Test
    void returnsEmptyListWhenProductHasNoRecommendations() {
        when(repository.findByProductId(7)).thenReturn(List.of());

        assertEquals(List.of(), service.getRecommendations(7));
    }

    @Test
    void rejectsNonPositiveProductId() {
        InvalidInputException exception = assertThrows(InvalidInputException.class,
                () -> service.getRecommendations(0));

        assertEquals("Invalid productId: 0", exception.getMessage());
    }

    @Test
    void mapsRateToMongoRatingAndBack() {
        Recommendation input = new Recommendation(7, 2, "Ada", 4, "Useful", null);
        RecommendationEntity saved = new RecommendationEntity(7, 2, "Ada", 4, "Useful");
        when(repository.save(org.mockito.ArgumentMatchers.any(RecommendationEntity.class))).thenReturn(saved);

        Recommendation result = service.createRecommendation(input);

        assertEquals(4, result.rate());
        assertEquals("127.0.0.1:7002", result.serviceAddress());
        verify(repository).save(org.mockito.ArgumentMatchers.argThat(entity -> entity.getRating() == 4));
    }
}
