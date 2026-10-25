package br.com.fatecararas.microservices.core.review.services;
import br.com.fatecararas.api.core.review.*;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class ReviewController implements ReviewService {
    private final ReviewOperations operations;
    public ReviewController(ReviewOperations operations) { this.operations=operations; }
    @Override public Review createReview(Review body) { return operations.createReview(body); }
    @Override public List<Review> getReviews(int productId) { return operations.getReviews(productId); }
    @Override public void deleteReviews(int productId) { operations.deleteReviews(productId); }
}
