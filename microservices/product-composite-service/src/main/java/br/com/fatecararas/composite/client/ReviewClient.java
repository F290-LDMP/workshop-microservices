package br.com.fatecararas.composite.client;

import br.com.fatecararas.api.core.review.ReviewService;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${app.services.review}")
public interface ReviewClient extends ReviewService { }
