package br.com.fatecararas.microservices.core.recommendation.services;
import br.com.fatecararas.microservices.core.recommendation.persistence.*;
import br.com.fatecararas.api.core.recommendation.Recommendation;
import br.com.fatecararas.api.core.DomainValidation;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.util.http.ServiceUtil;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
@Service
public class RecommendationOperations {
    private final RecommendationRepository repository;
    private final ServiceUtil serviceUtil;
    public RecommendationOperations(RecommendationRepository repository, ServiceUtil serviceUtil) { this.repository=repository; this.serviceUtil=serviceUtil; }
    public void validate(Recommendation body) { DomainValidation.id(body.getProductId()); DomainValidation.id(body.getRecommendationId()); DomainValidation.rate(body.getRate()); }
    public Recommendation createRecommendation(Recommendation body) {
        validate(body);
        try { return toDto(repository.save(new RecommendationEntity(body.getProductId(), body.getRecommendationId(), body.getAuthor(), body.getRate(), body.getContent()))); }
        catch (DataIntegrityViolationException ex) { throw new InvalidInputException("Duplicate recommendation business key"); }
    }
    public List<Recommendation> getRecommendations(int productId) {
        DomainValidation.id(productId);
        return repository.findByProductId(productId).stream().map(this::toDto).toList();
    }
    
    public void deleteRecommendations(int productId) { DomainValidation.id(productId); repository.deleteByProductId(productId); }
    private Recommendation toDto(RecommendationEntity e) { return new Recommendation(e.getProductId(), e.getRecommendationId(), e.getAuthor(), e.getRating(), e.getContent(), serviceUtil.getServerAddress()); }
}
