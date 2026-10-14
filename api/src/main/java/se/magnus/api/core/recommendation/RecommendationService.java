package se.magnus.api.core.recommendation;

import java.util.List;

public interface RecommendationService {

    Recommendation createRecommendation(Recommendation recommendation);

    List<Recommendation> getRecommendations(int productId);

    void deleteRecommendations(int productId);
}
