package br.com.fatecararas.microservices.core.review.services;
import br.com.fatecararas.microservices.core.review.persistence.*;
import br.com.fatecararas.api.core.review.Review;
import br.com.fatecararas.api.core.DomainValidation;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.util.http.ServiceUtil;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
@Service
public class ReviewOperations {
    private final ReviewRepository repository;
    private final ServiceUtil serviceUtil;
    public ReviewOperations(ReviewRepository repository, ServiceUtil serviceUtil) { this.repository=repository; this.serviceUtil=serviceUtil; }
    public void validate(Review body) { DomainValidation.id(body.getProductId()); DomainValidation.id(body.getReviewId());  }
    public Review createReview(Review body) {
        validate(body);
        try { return toDto(repository.saveAndFlush(new ReviewEntity(body.getProductId(), body.getReviewId(), body.getAuthor(), body.getSubject(), body.getContent()))); }
        catch (DataIntegrityViolationException ex) { throw new InvalidInputException("Duplicate review business key"); }
    }
    public List<Review> getReviews(int productId) {
        DomainValidation.id(productId);
        return repository.findByProductId(productId).stream().map(this::toDto).toList();
    }
    @org.springframework.transaction.annotation.Transactional
    public void deleteReviews(int productId) { DomainValidation.id(productId); repository.deleteByProductId(productId); }
    private Review toDto(ReviewEntity e) { return new Review(e.getProductId(), e.getReviewId(), e.getAuthor(), e.getSubject(), e.getContent(), serviceUtil.getServerAddress()); }
}
