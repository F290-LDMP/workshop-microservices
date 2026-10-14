package se.magnus.microservices.recommendation;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.exception.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository repository;
    private final ServiceUtil serviceUtil;

    public RecommendationServiceImpl(RecommendationRepository repository, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Recommendation createRecommendation(Recommendation recommendation) {
        validateProductId(recommendation.productId());
        if (recommendation.rate() < 0 || recommendation.rate() > 5) {
            throw new InvalidInputException("rate must be between 0 and 5");
        }
        try {
            RecommendationEntity saved = repository.save(toEntity(recommendation));
            return toDto(saved);
        } catch (DuplicateKeyException exception) {
            throw exception;
        }
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        validateProductId(productId);
        return repository.findByProductId(productId).stream().map(this::toDto).toList();
    }

    @Override
    public void deleteRecommendations(int productId) {
        validateProductId(productId);
        repository.deleteByProductId(productId);
    }

    private void validateProductId(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
    }

    private RecommendationEntity toEntity(Recommendation recommendation) {
        return new RecommendationEntity(
                recommendation.productId(),
                recommendation.recommendationId(),
                recommendation.author(),
                recommendation.rate(),
                recommendation.content());
    }

    private Recommendation toDto(RecommendationEntity entity) {
        return new Recommendation(
                entity.getProductId(),
                entity.getRecommendationId(),
                entity.getAuthor(),
                entity.getRating(),
                entity.getContent(),
                serviceUtil.getServiceAddress());
    }
}
