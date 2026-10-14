package se.magnus.microservices.recommendation;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecommendationRepository extends MongoRepository<RecommendationEntity, String> {
    List<RecommendationEntity> findByProductId(int productId);
    void deleteByProductId(int productId);
}
