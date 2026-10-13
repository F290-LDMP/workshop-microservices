package se.magnus.api.core.review.services;

import br.com.fatecararas.api.core.review.Review;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.util.http.ServiceUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.magnus.api.core.review.domain.entities.ReviewEntity;
import se.magnus.api.core.review.domain.repositories.ReviewRepository;
import java.util.List;

@Service
public class ReviewApplicationService {
    private final ReviewRepository repository;
    private final ServiceUtil serviceUtil;

    public ReviewApplicationService(ReviewRepository repository, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.serviceUtil = serviceUtil;
    }

    @Transactional(readOnly = true)
    public List<Review> getReviews(int productId) {
        validateProductId(productId);
        return repository.findByProductIdOrderByReviewIdAsc(productId).stream().map(this::toDto).toList();
    }

    @Transactional
    public Review createReview(Review body) {
        validateProductId(body.getProductId());
        if (body.getReviewId() < 1) throw new InvalidInputException("reviewId deve ser positivo");
        validateText(body.getAuthor(), "author", 255);
        validateText(body.getSubject(), "subject", 255);
        validateText(body.getContent(), "content", 2000);
        if (repository.existsByProductIdAndReviewId(body.getProductId(), body.getReviewId())) {
            throw duplicate(body);
        }
        ReviewEntity entity = new ReviewEntity(body.getProductId(), body.getReviewId(),
                body.getAuthor(), body.getSubject(), body.getContent());
        try {
            return toDto(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            // Validação prévia não impede inserções simultâneas; o banco garante unicidade.
            if (isUniqueViolation(ex)) throw duplicate(body);
            throw ex;
        }
    }

    @Transactional
    public void deleteReviews(int productId) {
        validateProductId(productId);
        repository.deleteByProductId(productId);
    }

    private Review toDto(ReviewEntity entity) {
        return new Review(entity.getProductId(), entity.getReviewId(), entity.getAuthor(),
                entity.getSubject(), entity.getContent(), serviceUtil.getServerAddress());
    }

    private void validateProductId(int id) {
        if (id < 1) throw new InvalidInputException("Invalid productId: " + id);
    }

    private void validateText(String text, String field, int max) {
        if (text == null || text.isBlank() || text.length() > max) {
            throw new InvalidInputException(field + " deve conter entre 1 e " + max + " caracteres");
        }
    }

    private InvalidInputException duplicate(Review body) {
        return new InvalidInputException("Review já existe para productId=" + body.getProductId()
                + " e reviewId=" + body.getReviewId());
    }

    private boolean isUniqueViolation(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql) {
                if ("23505".equals(sql.getSQLState()) || sql.getErrorCode() == 1062) return true;
            }
        }
        return false;
    }
}
