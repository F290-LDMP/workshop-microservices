package se.magnus.microservices.recommendation;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.recommendation.Recommendation;

@RestController
@RequestMapping("/recommendation")
public class RecommendationController {

    private final se.magnus.api.core.recommendation.RecommendationService service;

    public RecommendationController(se.magnus.api.core.recommendation.RecommendationService service) {
        this.service = service;
    }

    @PostMapping
    public Recommendation createRecommendation(@RequestBody Recommendation recommendation) {
        return service.createRecommendation(recommendation);
    }

    @GetMapping
    public List<Recommendation> getRecommendations(@RequestParam int productId) {
        return service.getRecommendations(productId);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteRecommendations(@RequestParam int productId) {
        service.deleteRecommendations(productId);
        return ResponseEntity.noContent().build();
    }
}
