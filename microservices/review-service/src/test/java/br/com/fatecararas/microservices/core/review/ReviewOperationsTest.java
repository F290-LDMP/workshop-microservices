package br.com.fatecararas.microservices.core.review;
import br.com.fatecararas.microservices.core.review.services.ReviewOperations;
import br.com.fatecararas.microservices.core.review.persistence.*;
import br.com.fatecararas.api.core.review.Review;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.util.http.ServiceUtil;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ReviewOperationsTest {
    final ReviewRepository repository = mock(ReviewRepository.class);
    final ServiceUtil util = mock(ServiceUtil.class);
    final ReviewOperations operations = new ReviewOperations(repository,util);
    Review valid() { return new Review(1,1,"Ana","Assunto","OK",null); }
    @Test void mapsPersistenceAndServiceAddress() {
        when(util.getServerAddress()).thenReturn("http://instance:7000");
        when(repository.saveAndFlush(any())).thenReturn(new ReviewEntity(1,1,"Ana","Assunto","OK"));
        var result = operations.createReview(valid());
        assertEquals(1,result.getReviewId());
        assertEquals("http://instance:7000",result.getServiceAddress());
        assertEquals("Assunto",result.getSubject());
    }
    @Test void emptyCollectionAndIdempotentDelete() {
        when(repository.findByProductId(1)).thenReturn(List.of());
        assertTrue(operations.getReviews(1).isEmpty());
        operations.deleteReviews(1); operations.deleteReviews(1);
        verify(repository,times(2)).deleteByProductId(1);
    }
    @Test void rejectsInvalidIdsBeforeDatabaseAccess() {
        var body = valid(); body.setReviewId(0);
        assertThrows(InvalidInputException.class, () -> operations.createReview(body));
        assertThrows(InvalidInputException.class, () -> operations.getReviews(0));
        assertThrows(InvalidInputException.class, () -> operations.deleteReviews(0));
        verifyNoInteractions(repository);
    }
}
