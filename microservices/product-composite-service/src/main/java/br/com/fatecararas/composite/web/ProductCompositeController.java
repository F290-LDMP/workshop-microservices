package br.com.fatecararas.composite.web;

import br.com.fatecararas.api.composite.product.ProductAggregate;
import br.com.fatecararas.api.composite.product.ProductCompositeService;
import br.com.fatecararas.composite.service.ProductCompositeIntegrationService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductCompositeController implements ProductCompositeService {
    private final ProductCompositeIntegrationService integration;

    public ProductCompositeController(ProductCompositeIntegrationService integration) {
        this.integration = integration;
    }

    @Override
    public void createProduct(ProductAggregate body) {
        integration.create(body);
    }

    @Override
    public ProductAggregate getProduct(int productId) {
        return integration.get(productId);
    }

    @Override
    public void deleteProduct(int productId) {
        integration.delete(productId);
    }
}
