package br.com.fatecararas.microservices.core.recommendation.services;
import br.com.fatecararas.api.core.recommendation.*;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class RecommendationController implements RecommendationService {
    private final RecommendationOperations operations;
    public RecommendationController(RecommendationOperations operations) { this.operations=operations; }
    @Override public Recommendation createRecommendation(Recommendation body) { return operations.createRecommendation(body); }
    @Override public List<Recommendation> getRecommendations(int productId) { return operations.getRecommendations(productId); }
    @Override public void deleteRecommendations(int productId) { operations.deleteRecommendations(productId); }
}
