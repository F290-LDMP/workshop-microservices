package br.com.fatecararas.microservices.core.recommendation;
import br.com.fatecararas.microservices.core.recommendation.services.RecommendationOperations;
import br.com.fatecararas.microservices.core.recommendation.persistence.*;
import br.com.fatecararas.api.core.recommendation.Recommendation;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.util.http.ServiceUtil;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RecommendationOperationsTest {
    final RecommendationRepository repository = mock(RecommendationRepository.class);
    final ServiceUtil util = mock(ServiceUtil.class);
    final RecommendationOperations operations = new RecommendationOperations(repository,util);
    Recommendation valid() { return new Recommendation(1,1,"Ana",5,"OK",null); }
    @Test void mapsPersistenceAndServiceAddress() {
        when(util.getServerAddress()).thenReturn("http://instance:7000");
        when(repository.save(any())).thenReturn(new RecommendationEntity(1,1,"Ana",5,"OK"));
        var result = operations.createRecommendation(valid());
        assertEquals(1,result.getRecommendationId());
        assertEquals("http://instance:7000",result.getServiceAddress());
        assertEquals(5,result.getRate());
    }
    @Test void emptyCollectionAndIdempotentDelete() {
        when(repository.findByProductId(1)).thenReturn(List.of());
        assertTrue(operations.getRecommendations(1).isEmpty());
        operations.deleteRecommendations(1); operations.deleteRecommendations(1);
        verify(repository,times(2)).deleteByProductId(1);
    }
    @Test void rejectsInvalidIdsBeforeDatabaseAccess() {
        var body = valid(); body.setRecommendationId(0);
        assertThrows(InvalidInputException.class, () -> operations.createRecommendation(body));
        assertThrows(InvalidInputException.class, () -> operations.getRecommendations(0));
        assertThrows(InvalidInputException.class, () -> operations.deleteRecommendations(0));
        verifyNoInteractions(repository);
    }
    @Test void rejectsInvalidRateBeforeDatabaseAccess() {
        var body = valid(); body.setRate(6);
        assertThrows(InvalidInputException.class, () -> operations.createRecommendation(body));
        verifyNoInteractions(repository);
    }
}
