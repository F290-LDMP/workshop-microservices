package br.com.fatecararas.microservices.core.review.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {
    List<ReviewEntity> findByProductId(int productId);
    void deleteByProductId(int productId);
}
