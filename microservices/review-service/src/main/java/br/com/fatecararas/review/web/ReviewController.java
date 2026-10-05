package br.com.fatecararas.review.web;

import br.com.fatecararas.api.core.review.Review;
import br.com.fatecararas.review.service.ReviewApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController implements br.com.fatecararas.api.core.review.ReviewService {
    private final ReviewApplicationService service;

    public ReviewController(ReviewApplicationService service) {
        this.service = service;
    }

    @Override
    public Review createReview(Review body) {
        return service.createReview(body);
    }

    @Override
    public List<Review> getReviews(int productId) {
        return service.getReviews(productId);
    }

    @Override
    public void deleteReviews(int productId) {
        service.deleteReviews(productId);
    }
}
