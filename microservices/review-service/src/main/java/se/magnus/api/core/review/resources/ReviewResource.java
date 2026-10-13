package se.magnus.api.core.review.resources;

import br.com.fatecararas.api.core.review.Review;
import br.com.fatecararas.api.core.review.ReviewService;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.review.services.ReviewApplicationService;
import java.util.List;

@RestController
public class ReviewResource implements ReviewService {
    private final ReviewApplicationService service;

    public ReviewResource(ReviewApplicationService service) { this.service = service; }

    @Override
    public List<Review> getReviews(int productId) { return service.getReviews(productId); }

    @Override
    public Review createReview(Review body) { return service.createReview(body); }

    @Override
    public void deleteReviews(int productId) { service.deleteReviews(productId); }
}
