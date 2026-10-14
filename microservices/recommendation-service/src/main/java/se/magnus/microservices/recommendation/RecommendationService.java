package se.magnus.microservices.recommendation;

import java.util.List;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final RecommendationRepository repository;
    private final ServiceAddress serviceAddress;
    private final Registration registration;

    public RecommendationService(RecommendationRepository repository, ServiceAddress serviceAddress,
                                 org.springframework.beans.factory.ObjectProvider<Registration> registrationProvider) {
        this.repository = repository;
        this.serviceAddress = serviceAddress;
        this.registration = registrationProvider.getIfAvailable();
    }

    public Recommendation create(Recommendation recommendation) {
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

    public List<Recommendation> findByProductId(int productId) {
        validateProductId(productId);
        return repository.findByProductId(productId).stream().map(this::toDto).toList();
    }

    public void deleteByProductId(int productId) {
        validateProductId(productId);
        repository.deleteByProductId(productId);
    }

    private void validateProductId(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
    }

    private RecommendationEntity toEntity(Recommendation recommendation) {
        return new RecommendationEntity(recommendation.productId(), recommendation.recommendationId(),
                recommendation.author(), recommendation.rate(), recommendation.content());
    }

    private Recommendation toDto(RecommendationEntity entity) {
        return new Recommendation(entity.getProductId(), entity.getRecommendationId(), entity.getAuthor(),
                entity.getRating(), entity.getContent(), getServiceAddress());
    }

    private String getServiceAddress() {
        return registration == null ? serviceAddress.getAddress()
                : registration.getHost() + ":" + registration.getPort();
    }
}
