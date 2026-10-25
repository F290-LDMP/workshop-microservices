package br.com.fatecararas.microservices.core.product.services;
import br.com.fatecararas.api.core.product.*;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class ProductServiceImpl implements ProductService {
    private final ProductOperations operations;
    public ProductServiceImpl(ProductOperations operations) { this.operations=operations; }
    public Product createProduct(Product body) { return operations.createProduct(body); }
    public Product getProduct(int productId) { return operations.getProduct(productId); }
    public void deleteProduct(int productId) { operations.deleteProduct(productId); }
}
