package br.com.fatecararas.review.service;

import br.com.fatecararas.api.core.review.Review;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.review.domain.ReviewEntity;
import br.com.fatecararas.review.domain.ReviewRepository;
import br.com.fatecararas.util.http.ServiceUtil;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewApplicationService {
    private final ReviewRepository repository;
    private final ServiceUtil serviceUtil;

    public ReviewApplicationService(ReviewRepository repository, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.serviceUtil = serviceUtil;
    }

    @Transactional
    public Review createReview(Review review) {
        validate(review);
        if (repository.existsByProductIdAndReviewId(review.getProductId(), review.getReviewId())) {
            throw new InvalidInputException("Já existe um review com esse productId e reviewId");
        }
        ReviewEntity saved = repository.save(new ReviewEntity(review.getProductId(), review.getReviewId(),
                review.getAuthor(), review.getSubject(), review.getContent()));
        return toApi(saved);
    }

    @Transactional(readOnly = true)
    public List<Review> getReviews(int productId) {
        requirePositiveProductId(productId);
        return repository.findByProductId(productId).stream().map(this::toApi).toList();
    }

    @Transactional
    public void deleteReviews(int productId) {
        requirePositiveProductId(productId);
        repository.deleteByProductId(productId);
    }

    private Review toApi(ReviewEntity entity) {
        return new Review(entity.getProductId(), entity.getReviewId(), entity.getAuthor(), entity.getSubject(),
                entity.getContent(), serviceUtil.getServerAddress());
    }

    private void validate(Review review) {
        if (review == null) {
            throw new InvalidInputException("O corpo do review é obrigatório");
        }
        requirePositiveProductId(review.getProductId());
        if (review.getReviewId() < 1) {
            throw new InvalidInputException("reviewId deve ser maior que zero");
        }
        if (isBlank(review.getAuthor()) || isBlank(review.getSubject()) || isBlank(review.getContent())) {
            throw new InvalidInputException("author, subject e content são obrigatórios");
        }
    }

    private void requirePositiveProductId(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("productId deve ser maior que zero");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
