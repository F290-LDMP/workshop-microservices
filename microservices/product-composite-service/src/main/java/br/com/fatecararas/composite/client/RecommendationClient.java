package br.com.fatecararas.composite.client;

import br.com.fatecararas.api.core.recommendation.RecommendationService;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${app.services.recommendation}")
public interface RecommendationClient extends RecommendationService { }
