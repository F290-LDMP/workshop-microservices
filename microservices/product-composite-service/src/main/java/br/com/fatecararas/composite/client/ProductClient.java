package br.com.fatecararas.composite.client;

import br.com.fatecararas.api.core.product.ProductService;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${app.services.product}")
public interface ProductClient extends ProductService { }
