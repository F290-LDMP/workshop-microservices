package br.com.fatecararas.review.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {
    List<ReviewEntity> findByProductId(int productId);
    boolean existsByProductIdAndReviewId(int productId, int reviewId);
    long deleteByProductId(int productId);
}
