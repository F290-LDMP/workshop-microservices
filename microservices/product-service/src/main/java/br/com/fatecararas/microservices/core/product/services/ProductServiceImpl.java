package br.com.fatecararas.microservices.core.product.services;

import br.com.fatecararas.api.core.product.Product;
import br.com.fatecararas.api.core.product.ProductService;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.api.exceptions.NotFoundException;
import br.com.fatecararas.microservices.core.product.persistence.ProductEntity;
import br.com.fatecararas.microservices.core.product.persistence.ProductRepository;
import br.com.fatecararas.util.http.ServiceUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repository;
    private final ServiceUtil serviceUtil;

    public ProductServiceImpl(ProductRepository repository, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Product createProduct(Product product) {
        validateProductId(product.getProductId());
        try {
            return toProduct(repository.save(new ProductEntity(product.getProductId(), product.getName(), product.getWeight())));
        } catch (DuplicateKeyException exception) {
            throw new InvalidInputException("Duplicate productId: " + product.getProductId());
        }
    }

    @Override
    public Product getProduct(int productId) {
        validateProductId(productId);
        return repository.findByProductId(productId).map(this::toProduct)
                .orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));
    }

    @Override
    public void deleteProduct(int productId) {
        validateProductId(productId);
        repository.findByProductId(productId).ifPresent(repository::delete);
    }

    private void validateProductId(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
    }

    private Product toProduct(ProductEntity entity) {
        return new Product(entity.getProductId(), entity.getName(), entity.getWeight(), serviceUtil.getServerAddress());
    }
}
