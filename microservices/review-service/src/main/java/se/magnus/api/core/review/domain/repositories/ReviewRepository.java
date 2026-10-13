package se.magnus.api.core.review.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.magnus.api.core.review.domain.entities.ReviewEntity;
import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {
    List<ReviewEntity> findByProductIdOrderByReviewIdAsc(int productId);
    boolean existsByProductIdAndReviewId(int productId, int reviewId);
    void deleteByProductId(int productId);
}
